package be.immoconnect.services;

import be.immoconnect.entities.Jeton;
import be.immoconnect.entities.TypeJeton;
import be.immoconnect.entities.Utilisateur;
import be.immoconnect.exceptions.CodeInvalideException;
import be.immoconnect.exceptions.JetonInvalideException;
import be.immoconnect.repositories.JetonRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Jetons à usage unique : session (rafraîchissement), activation d'un compte, réinitialisation du mot
 * de passe et code du double facteur. La valeur remise à l'utilisateur est tirée au hasard sur 256 bits ;
 * seule son empreinte SHA-256 est enregistrée.
 */
@Service
public class ServiceJetonsUniques {

    public static final Duration DUREE_ACTIVATION = Duration.ofHours(24);
    public static final Duration DUREE_REINITIALISATION = Duration.ofMinutes(30);
    public static final Duration DUREE_DEFI = Duration.ofMinutes(10);

    private static final Logger journal = LoggerFactory.getLogger(ServiceJetonsUniques.class);
    private static final SecureRandom ALEA = new SecureRandom();

    /** Défi d'un double facteur : la valeur à renvoyer avec le code, et le code lui-même à envoyer par e-mail. */
    public record Defi(String valeur, String code) {
    }

    private final JetonRepository jetons;
    private final FermetureDesSessions fermeture;
    private final Clock horloge;

    public ServiceJetonsUniques(JetonRepository jetons, FermetureDesSessions fermeture, Clock horloge) {
        this.jetons = jetons;
        this.fermeture = fermeture;
        this.horloge = horloge;
    }

    /** @return la valeur du jeton, à remettre à l'utilisateur et à ne jamais enregistrer */
    @Transactional
    public String emettre(Utilisateur utilisateur, TypeJeton type, Duration duree, String ip) {
        String valeur = aleatoire();
        LocalDateTime maintenant = maintenant();
        jetons.save(new Jeton(utilisateur, type, empreinte(valeur), null, maintenant, maintenant.plus(duree), ip));
        return valeur;
    }

    /** Le code à six chiffres n'est jamais enregistré non plus : seule son empreinte l'est. */
    @Transactional
    public Defi emettreDefi(Utilisateur utilisateur, String ip) {
        String valeur = aleatoire();
        String code = String.format("%06d", ALEA.nextInt(1_000_000));
        LocalDateTime maintenant = maintenant();
        jetons.save(new Jeton(utilisateur, TypeJeton.double_facteur, empreinte(valeur), empreinte(code),
                maintenant, maintenant.plus(DUREE_DEFI), ip));
        return new Defi(valeur, code);
    }

    /**
     * Consomme un jeton : il devient inutilisable. Un jeton de session présenté deux fois trahit un
     * vol de cookie : toutes les sessions de l'utilisateur sont alors fermées.
     */
    @Transactional
    public Jeton consommer(String valeur, TypeJeton type) {
        Jeton jeton = jetons.findByEmpreinteAndType(empreinte(valeur == null ? "" : valeur), type)
                .orElseThrow(JetonInvalideException::new);
        LocalDateTime maintenant = maintenant();
        if (type == TypeJeton.rafraichissement && jeton.dejaUtilise()) {
            journal.warn("Jeton de session réutilisé pour l'utilisateur {} : toutes ses sessions sont fermées",
                    jeton.getUtilisateur().getId());
            fermeture.fermerToutes(jeton.getUtilisateur().getId());
            throw new JetonInvalideException();
        }
        if (!jeton.estUtilisable(maintenant)) {
            throw new JetonInvalideException();
        }
        jeton.utiliser(maintenant);
        return jeton;
    }

    /** Vérifie le code d'un défi ; chaque erreur compte, et le défi expire après cinq erreurs. */
    @Transactional(noRollbackFor = CodeInvalideException.class)
    public Jeton verifierCode(String defi, String code) {
        Jeton jeton = jetons.findByEmpreinteAndType(empreinte(defi == null ? "" : defi), TypeJeton.double_facteur)
                .orElseThrow(JetonInvalideException::new);
        LocalDateTime maintenant = maintenant();
        if (!jeton.estUtilisable(maintenant)) {
            throw new JetonInvalideException();
        }
        if (code == null || !MessageDigest.isEqual(jeton.getEmpreinteCode().getBytes(StandardCharsets.US_ASCII),
                empreinte(code.strip()).getBytes(StandardCharsets.US_ASCII))) {
            jeton.compterEssai();
            throw new CodeInvalideException(Jeton.ESSAIS_MAX - jeton.getEssais());
        }
        jeton.utiliser(maintenant);
        return jeton;
    }

    @Transactional
    public void fermerTous(Integer utilisateurId, TypeJeton type) {
        jetons.fermerTous(utilisateurId, type, maintenant());
    }

    /** Chaque nuit, les jetons expirés depuis plus de trente jours disparaissent. */
    @Scheduled(cron = "0 15 4 * * *", zone = "Europe/Brussels")
    @Transactional
    public void purger() {
        int supprimes = jetons.supprimerExpiresAvant(maintenant().minusDays(30));
        if (supprimes > 0) {
            journal.info("Jetons expirés purgés : {}", supprimes);
        }
    }

    public static String empreinte(String valeur) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valeur.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    private static String aleatoire() {
        byte[] octets = new byte[32];
        ALEA.nextBytes(octets);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
    }

    private LocalDateTime maintenant() {
        return LocalDateTime.now(horloge);
    }
}
