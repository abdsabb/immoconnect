package be.immoconnect.paiement;

/** Notification du prestataire, réduite à ce que l'application en fait. */
public record EvenementPaiement(Type type, String intentionId) {

    public enum Type {
        paiement_reussi, paiement_echoue, paiement_rembourse
    }
}
