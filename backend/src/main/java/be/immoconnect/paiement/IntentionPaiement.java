package be.immoconnect.paiement;

import java.util.Map;

/**
 * Intention de paiement (PaymentIntent chez Stripe) : le montant à régler et son état.
 * Le {@code clientSecret} permet au navigateur de saisir la carte pour cette intention, et elle seule.
 */
public record IntentionPaiement(String id, String clientSecret, long montantCentimes, String devise, Statut statut,
                                Map<String, String> metadonnees) {

    public enum Statut {
        en_attente, reussie, annulee
    }

    public boolean estReussie() {
        return statut == Statut.reussie;
    }
}
