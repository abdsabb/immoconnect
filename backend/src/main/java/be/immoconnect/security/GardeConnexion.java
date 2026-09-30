package be.immoconnect.security;

import be.immoconnect.entities.Utilisateur;
import be.immoconnect.exceptions.TropDeTentativesException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Parade à la force brute sur la connexion (livrable 16, §2.2), à deux niveaux :
 * <ul>
 *   <li>par adresse IP : un nombre de tentatives par minute, quel que soit le compte visé ;</li>
 *   <li>par compte : après quelques échecs, le compte est verrouillé, de plus en plus longtemps.</li>
 * </ul>
 * Une adresse e-mail inconnue se verrouille comme une adresse connue : le comportement de la
 * connexion ne révèle donc pas quels comptes existent.
 */
@Component
public class GardeConnexion {

    /** Premier verrouillage d'une minute, doublé à chaque nouvel échec, plafonné à un quart d'heure. */
    private static final Duration VERROU_INITIAL = Duration.ofMinutes(1);
    private static final Duration VERROU_MAX = Duration.ofMinutes(15);
    /** Au-delà, les compteurs en mémoire sont purgés : la mémoire reste bornée, même sous attaque. */
    private static final int ENTREES_MAX = 10_000;

    private record Fenetre(long minute, int appels) {
    }

    private record Echecs(int nombre, LocalDateTime verrouilleJusquA) {
    }

    private final ConcurrentHashMap<String, Fenetre> parAdresseIp = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Echecs> adressesInconnues = new ConcurrentHashMap<>();
    private final ProprietesSecurite proprietes;
    private final DetectionIntrusion detection;
    private final Clock horloge;

    public GardeConnexion(ProprietesSecurite proprietes, DetectionIntrusion detection, Clock horloge) {
        this.proprietes = proprietes;
        this.detection = detection;
        this.horloge = horloge;
    }

    /** Refuse la tentative (429) si l'adresse IP est bannie par la détection d'intrusion ou a épuisé son quota de la minute. */
    public void admettre(String ip) {
        long bannissement = detection.secondesDeBannissement(ip);
        if (bannissement > 0) {
            throw new TropDeTentativesException(bannissement);
        }
        long minute = horloge.millis() / 60_000;
        if (parAdresseIp.size() > ENTREES_MAX) {
            parAdresseIp.values().removeIf(fenetre -> fenetre.minute() < minute);
        }
        Fenetre fenetre = parAdresseIp.merge(ip == null ? "inconnue" : ip, new Fenetre(minute, 1),
                (ancienne, nouvelle) -> ancienne.minute() == minute ? new Fenetre(minute, ancienne.appels() + 1) : nouvelle);
        if (fenetre.appels() > proprietes.connexionsParMinute()) {
            throw new TropDeTentativesException(60 - (horloge.millis() / 1000) % 60);
        }
    }

    /** Refuse la tentative (429) tant que le compte, connu ou non, est verrouillé. */
    public void admettre(String email, Utilisateur compte) {
        LocalDateTime maintenant = maintenant();
        LocalDateTime verrou = compte != null ? compte.getVerrouilleJusquA()
                : adressesInconnues.getOrDefault(cle(email), new Echecs(0, null)).verrouilleJusquA();
        if (verrou != null && verrou.isAfter(maintenant)) {
            throw new TropDeTentativesException(Duration.between(maintenant, verrou).toSeconds() + 1);
        }
    }

    /** Un échec de plus : à partir du seuil, le compte se verrouille ; la détection d'intrusion compte aussi par adresse IP. */
    public void noterEchec(String ip, String email, Utilisateur compte) {
        detection.echecConnexion(ip, email);
        if (compte != null) {
            int echecs = Math.min(compte.getEchecsConnexion() + 1, 100);
            compte.setEchecsConnexion(echecs);
            compte.setVerrouilleJusquA(verrou(echecs));
            return;
        }
        if (adressesInconnues.size() > ENTREES_MAX) {
            adressesInconnues.clear();
        }
        adressesInconnues.merge(cle(email), new Echecs(1, verrou(1)),
                (ancien, nouveau) -> new Echecs(ancien.nombre() + 1, verrou(ancien.nombre() + 1)));
    }

    /** Une connexion réussie efface l'ardoise. */
    public void noterSucces(Utilisateur compte) {
        compte.setEchecsConnexion(0);
        compte.setVerrouilleJusquA(null);
    }

    private LocalDateTime verrou(int echecs) {
        int depassement = echecs - proprietes.echecsAvantVerrouillage();
        if (depassement < 0) {
            return null;
        }
        long minutes = Math.min(VERROU_MAX.toMinutes(), VERROU_INITIAL.toMinutes() << Math.min(depassement, 10));
        return maintenant().plusMinutes(minutes);
    }

    private LocalDateTime maintenant() {
        return LocalDateTime.now(horloge);
    }

    private static String cle(String email) {
        return email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    }
}
