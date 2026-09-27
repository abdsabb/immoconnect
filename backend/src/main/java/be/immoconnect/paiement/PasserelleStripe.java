package be.immoconnect.paiement;

import com.stripe.StripeClient;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentCreateParams.AutomaticPaymentMethods;
import com.stripe.param.RefundCreateParams;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Adaptateur Stripe : traduit les appels de l'application vers l'API PaymentIntents et Refunds. */
public class PasserelleStripe implements PasserellePaiement {

    private static final Logger journal = LoggerFactory.getLogger(PasserelleStripe.class);
    private static final String DEVISE = "eur";

    private final StripeClient stripe;
    private final ProprietesStripe proprietes;
    private final ObjectMapper json;

    public PasserelleStripe(ProprietesStripe proprietes, ObjectMapper json) {
        this.stripe = new StripeClient(proprietes.cleSecrete());
        this.proprietes = proprietes;
        this.json = json;
    }

    @Override
    public String mode() {
        return "stripe";
    }

    @Override
    public String clePublique() {
        return proprietes.clePublique();
    }

    @Override
    public IntentionPaiement creerIntention(long montantCentimes, String description, String emailRecu,
                                            Map<String, String> metadonnees) {
        PaymentIntentCreateParams parametres = PaymentIntentCreateParams.builder()
                .setAmount(montantCentimes)
                .setCurrency(DEVISE)
                .setDescription(description)
                .setReceiptEmail(emailRecu)
                .putAllMetadata(metadonnees)
                // Paiement par carte sans quitter la page : pas de moyen de paiement à redirection.
                .setAutomaticPaymentMethods(AutomaticPaymentMethods.builder()
                        .setEnabled(true)
                        .setAllowRedirects(AutomaticPaymentMethods.AllowRedirects.NEVER)
                        .build())
                .build();
        try {
            return traduire(stripe.v1().paymentIntents().create(parametres));
        } catch (StripeException e) {
            throw indisponible("création du paiement", e);
        }
    }

    @Override
    public IntentionPaiement consulter(String id) {
        try {
            return traduire(stripe.v1().paymentIntents().retrieve(id));
        } catch (InvalidRequestException e) {
            throw new IntentionIntrouvableException(id);
        } catch (StripeException e) {
            throw indisponible("consultation du paiement", e);
        }
    }

    @Override
    public void rembourser(String id) {
        try {
            stripe.v1().refunds().create(RefundCreateParams.builder().setPaymentIntent(id).build());
        } catch (StripeException e) {
            throw indisponible("remboursement", e);
        }
    }

    /**
     * La signature (en-tête Stripe-Signature, HMAC-SHA256 du corps brut et de l'horodatage) prouve que la
     * notification vient de Stripe et n'a pas été rejouée. De l'événement, seule la référence du paiement
     * est retenue : son état réel est toujours relu auprès de Stripe avant toute écriture.
     */
    @Override
    public Optional<EvenementPaiement> lireEvenement(String charge, String signature) {
        Event evenement;
        try {
            evenement = stripe.constructEvent(charge, signature, proprietes.secretWebhook());
        } catch (SignatureVerificationException | RuntimeException e) {
            throw new SignatureInvalideException("Signature du webhook invalide");
        }
        JsonNode objet = json.readTree(evenement.getDataObjectDeserializer().getRawJson());
        return switch (evenement.getType()) {
            case "payment_intent.succeeded" -> evenement(EvenementPaiement.Type.paiement_reussi, objet.path("id"));
            case "payment_intent.payment_failed" -> evenement(EvenementPaiement.Type.paiement_echoue, objet.path("id"));
            case "charge.refunded" -> evenement(EvenementPaiement.Type.paiement_rembourse, objet.path("payment_intent"));
            default -> Optional.empty();
        };
    }

    private static Optional<EvenementPaiement> evenement(EvenementPaiement.Type type, JsonNode intentionId) {
        return intentionId.isString() ? Optional.of(new EvenementPaiement(type, intentionId.asString())) : Optional.empty();
    }

    private static IntentionPaiement traduire(PaymentIntent intention) {
        IntentionPaiement.Statut statut = switch (intention.getStatus()) {
            case "succeeded" -> IntentionPaiement.Statut.reussie;
            case "canceled" -> IntentionPaiement.Statut.annulee;
            default -> IntentionPaiement.Statut.en_attente;
        };
        return new IntentionPaiement(intention.getId(), intention.getClientSecret(), intention.getAmount(),
                intention.getCurrency(), statut, Map.copyOf(intention.getMetadata()));
    }

    private static PasserelleIndisponibleException indisponible(String operation, StripeException cause) {
        // Le détail reste dans les journaux du serveur : rien de technique ne part vers le client.
        journal.error("Stripe — échec de l'opération « {} » : {}", operation, cause.getMessage());
        return new PasserelleIndisponibleException("Le service de paiement est momentanément indisponible", cause);
    }
}
