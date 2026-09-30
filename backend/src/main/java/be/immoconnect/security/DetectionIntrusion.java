package be.immoconnect.security;

import be.immoconnect.entities.TypeAlerte;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Détection d'intrusion au niveau applicatif (chapitre 9.6 du rapport) : des seuils sur les événements de
 * sécurité déclenchent une alerte et, pour la connexion, un bannissement temporaire de l'adresse.
 * <ul>
 *   <li>rafale d'échecs de connexion depuis une même adresse IP ;</li>
 *   <li>énumération d'identifiants : une même adresse essaie de nombreux comptes différents ;</li>
 *   <li>usage d'une clé API révoquée.</li>
 * </ul>
 * Les compteurs vivent en mémoire, dans une fenêtre glissante ; l'alerte, elle, est enregistrée, écrite au
 * journal du serveur (où fail2ban peut la lire) et envoyée aux administrateurs.
 */
@Component
public class DetectionIntrusion {

    /** Alerte publiée dans l'application ; son enregistrement et son envoi se font à part, sans ralentir la requête. */
    public record Alerte(TypeAlerte type, String ip, String detail) {
    }

    /** Une même alerte (type et adresse) n'est pas répétée plus d'une fois par heure. */
    private static final Duration SILENCE = Duration.ofHours(1);
    private static final int ENTREES_MAX = 10_000;

    private record Tentative(Instant quand, String email) {
    }

    private final ConcurrentHashMap<String, Deque<Tentative>> echecsParIp = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instant> bannies = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instant> dernieresAlertes = new ConcurrentHashMap<>();
    private final ApplicationEventPublisher evenements;
    private final Clock horloge;
    private final int seuilEchecs;
    private final int seuilComptes;
    private final Duration fenetre;
    private final Duration bannissement;

    public DetectionIntrusion(ApplicationEventPublisher evenements, Clock horloge,
                              @Value("${immoconnect.securite.intrusion.echecs:10}") int seuilEchecs,
                              @Value("${immoconnect.securite.intrusion.comptes:5}") int seuilComptes,
                              @Value("${immoconnect.securite.intrusion.fenetre:10m}") Duration fenetre,
                              @Value("${immoconnect.securite.intrusion.bannissement:15m}") Duration bannissement) {
        this.evenements = evenements;
        this.horloge = horloge;
        this.seuilEchecs = seuilEchecs;
        this.seuilComptes = seuilComptes;
        this.fenetre = fenetre;
        this.bannissement = bannissement;
    }

    /** @return le nombre de secondes pendant lesquelles cette adresse reste bannie, 0 si elle ne l'est pas */
    public long secondesDeBannissement(String ip) {
        Instant fin = bannies.get(cle(ip));
        if (fin == null) {
            return 0;
        }
        Instant maintenant = horloge.instant();
        if (!fin.isAfter(maintenant)) {
            bannies.remove(cle(ip));
            return 0;
        }
        return Duration.between(maintenant, fin).toSeconds() + 1;
    }

    /** Un échec de connexion, que le compte visé existe ou non. */
    public void echecConnexion(String ip, String email) {
        Instant maintenant = horloge.instant();
        if (echecsParIp.size() > ENTREES_MAX) {
            echecsParIp.clear();
        }
        Deque<Tentative> tentatives = echecsParIp.computeIfAbsent(cle(ip), k -> new ArrayDeque<>());
        int echecs;
        long comptes;
        synchronized (tentatives) {
            tentatives.addLast(new Tentative(maintenant, email == null ? "" : email.strip().toLowerCase(Locale.ROOT)));
            while (!tentatives.isEmpty() && tentatives.peekFirst().quand().isBefore(maintenant.minus(fenetre))) {
                tentatives.removeFirst();
            }
            echecs = tentatives.size();
            comptes = tentatives.stream().map(Tentative::email).distinct().count();
        }
        if (comptes >= seuilComptes) {
            bannir(ip, maintenant);
            alerter(TypeAlerte.enumeration, ip, comptes + " comptes différents essayés en " + fenetre.toMinutes() + " min", maintenant);
        } else if (echecs >= seuilEchecs) {
            bannir(ip, maintenant);
            alerter(TypeAlerte.rafale_echecs, ip, echecs + " échecs de connexion en " + fenetre.toMinutes() + " min", maintenant);
        }
    }

    /** Une clé API révoquée a été présentée : elle a pu fuiter avant sa révocation (RA12). */
    public void cleRevoquee(String ip, String libelle) {
        alerter(TypeAlerte.cle_revoquee, ip, "clé « " + (libelle == null ? "sans libellé" : libelle) + " »", horloge.instant());
    }

    private void bannir(String ip, Instant maintenant) {
        bannies.put(cle(ip), maintenant.plus(bannissement));
    }

    private void alerter(TypeAlerte type, String ip, String detail, Instant maintenant) {
        String cle = type + "|" + cle(ip);
        Instant derniere = dernieresAlertes.get(cle);
        if (derniere != null && derniere.plus(SILENCE).isAfter(maintenant)) {
            return;
        }
        if (dernieresAlertes.size() > ENTREES_MAX) {
            dernieresAlertes.clear();
        }
        dernieresAlertes.put(cle, maintenant);
        evenements.publishEvent(new Alerte(type, cle(ip), detail));
    }

    private static String cle(String ip) {
        return ip == null || ip.isBlank() ? "inconnue" : ip;
    }
}
