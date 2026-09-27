package be.immoconnect.paiement;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Clés Stripe, fournies par l'environnement (jamais versionnées) — préfixe immoconnect.stripe. */
@ConfigurationProperties("immoconnect.stripe")
public record ProprietesStripe(String cleSecrete, String clePublique, String secretWebhook) {

    public boolean estConfigure() {
        return cleSecrete != null && !cleSecrete.isBlank();
    }
}
