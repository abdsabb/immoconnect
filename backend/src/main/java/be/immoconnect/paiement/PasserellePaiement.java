package be.immoconnect.paiement;

import java.util.Map;
import java.util.Optional;

/**
 * Prestataire de paiement vu par l'application (pattern Adapter, analyse UML §3.7).
 * Le reste du code ne connaît que cette interface : changer de prestataire ne toucherait que
 * l'adaptateur, et les tests remplacent Stripe par {@link PasserelleSimulee} sans appel réseau.
 */
public interface PasserellePaiement {

    /** « stripe » ou « simulation » : l'interface affiche le formulaire de paiement correspondant. */
    String mode();

    /** Clé publiable transmise au navigateur ; elle n'autorise que la saisie d'un paiement. */
    String clePublique();

    IntentionPaiement creerIntention(long montantCentimes, String description, String emailRecu, Map<String, String> metadonnees);

    /** @throws IntentionIntrouvableException si la référence n'existe pas chez le prestataire */
    IntentionPaiement consulter(String id);

    void rembourser(String id);

    /**
     * Authentifie une notification du prestataire (webhook) par sa signature, puis la traduit.
     * Vide si l'événement ne concerne pas l'application.
     *
     * @throws SignatureInvalideException si la notification ne vient pas du prestataire
     */
    Optional<EvenementPaiement> lireEvenement(String charge, String signature);

    class IntentionIntrouvableException extends RuntimeException {
        public IntentionIntrouvableException(String id) {
            super("Paiement " + id + " introuvable");
        }
    }

    class SignatureInvalideException extends RuntimeException {
        public SignatureInvalideException(String message) {
            super(message);
        }
    }

    /** Le prestataire ne répond pas ou refuse l'appel : l'opération en cours est abandonnée (réponse 502). */
    class PasserelleIndisponibleException extends RuntimeException {
        public PasserelleIndisponibleException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
