package be.immoconnect.paiement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import be.immoconnect.paiement.PasserellePaiement.SignatureInvalideException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Vérification de la signature des webhooks par l'adaptateur Stripe. Le test signe lui-même ses
 * notifications comme le fait Stripe (HMAC-SHA256 de « horodatage.corps ») : aucun appel réseau.
 */
class PasserelleStripeTest {

    private static final String SECRET = "whsec_test_immoconnect";

    private final PasserelleStripe passerelle = new PasserelleStripe(
            new ProprietesStripe("sk_test_immoconnect", "pk_test_immoconnect", SECRET, null), new ObjectMapper());

    @Test
    void uneNotificationSigneeDePaiementReussiEstTraduite() {
        String charge = evenement("payment_intent.succeeded", "{\"id\":\"pi_3Test\",\"object\":\"payment_intent\"}");

        assertThat(passerelle.lireEvenement(charge, signer(charge, SECRET, Instant.now())))
                .contains(new EvenementPaiement(EvenementPaiement.Type.paiement_reussi, "pi_3Test"));
    }

    @Test
    void unRemboursementRenvoieAuPaiementDeLaCharge() {
        String charge = evenement("charge.refunded", "{\"id\":\"ch_3Test\",\"object\":\"charge\",\"payment_intent\":\"pi_3Test\"}");

        assertThat(passerelle.lireEvenement(charge, signer(charge, SECRET, Instant.now())))
                .contains(new EvenementPaiement(EvenementPaiement.Type.paiement_rembourse, "pi_3Test"));
    }

    @Test
    void unEvenementSansInteretEstIgnore() {
        String charge = evenement("customer.created", "{\"id\":\"cus_Test\",\"object\":\"customer\"}");

        assertThat(passerelle.lireEvenement(charge, signer(charge, SECRET, Instant.now()))).isEmpty();
    }

    @Test
    void uneNotificationSigneeAvecUnAutreSecretEstRefusee() {
        String charge = evenement("payment_intent.succeeded", "{\"id\":\"pi_3Test\",\"object\":\"payment_intent\"}");

        assertThatThrownBy(() -> passerelle.lireEvenement(charge, signer(charge, "whsec_pirate", Instant.now())))
                .isInstanceOf(SignatureInvalideException.class);
    }

    @Test
    void uneNotificationModifieeApresSignatureEstRefusee() {
        String charge = evenement("payment_intent.succeeded", "{\"id\":\"pi_3Test\",\"object\":\"payment_intent\"}");
        String signature = signer(charge, SECRET, Instant.now());

        assertThatThrownBy(() -> passerelle.lireEvenement(charge.replace("pi_3Test", "pi_3Autre"), signature))
                .isInstanceOf(SignatureInvalideException.class);
    }

    /** Protection contre le rejeu : Stripe tolère cinq minutes d'écart, pas une heure. */
    @Test
    void uneNotificationRejoueeUneHeurePlusTardEstRefusee() {
        String charge = evenement("payment_intent.succeeded", "{\"id\":\"pi_3Test\",\"object\":\"payment_intent\"}");

        assertThatThrownBy(() -> passerelle.lireEvenement(charge, signer(charge, SECRET, Instant.now().minusSeconds(3600))))
                .isInstanceOf(SignatureInvalideException.class);
    }

    @Test
    void uneNotificationSansSignatureEstRefusee() {
        String charge = evenement("payment_intent.succeeded", "{\"id\":\"pi_3Test\",\"object\":\"payment_intent\"}");

        assertThatThrownBy(() -> passerelle.lireEvenement(charge, null)).isInstanceOf(SignatureInvalideException.class);
    }

    private static String evenement(String type, String objet) {
        return "{\"id\":\"evt_test\",\"object\":\"event\",\"type\":\"" + type + "\",\"data\":{\"object\":" + objet + "}}";
    }

    private static String signer(String charge, String secret, Instant instant) {
        try {
            long horodatage = instant.getEpochSecond();
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] empreinte = hmac.doFinal((horodatage + "." + charge).getBytes(StandardCharsets.UTF_8));
            return "t=" + horodatage + ",v1=" + HexFormat.of().formatHex(empreinte);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
