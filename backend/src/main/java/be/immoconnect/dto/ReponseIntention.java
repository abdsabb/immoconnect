package be.immoconnect.dto;

import be.immoconnect.paiement.IntentionPaiement;
import java.math.BigDecimal;

/** Ce dont le navigateur a besoin pour afficher le formulaire de carte et régler le créneau. */
public record ReponseIntention(String paymentIntentId, String clientSecret, BigDecimal montant, String devise) {

    public static ReponseIntention depuis(IntentionPaiement intention) {
        return new ReponseIntention(intention.id(), intention.clientSecret(),
                BigDecimal.valueOf(intention.montantCentimes(), 2), intention.devise());
    }
}
