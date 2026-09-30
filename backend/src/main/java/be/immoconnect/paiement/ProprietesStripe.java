package be.immoconnect.paiement;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Clés Stripe, fournies par l'environnement (jamais versionnées) — préfixe immoconnect.stripe.
 * Les moyens de paiement proposés (carte, Bancontact) sont réglables : Bancontact, très utilisé en
 * Belgique, passe par une redirection vers la banque puis un retour sur le site.
 */
@ConfigurationProperties("immoconnect.stripe")
public record ProprietesStripe(String cleSecrete, String clePublique, String secretWebhook, List<String> moyens) {

    public static final String CARTE = "card";
    public static final String BANCONTACT = "bancontact";

    public List<String> moyens() {
        return moyens == null || moyens.isEmpty() ? List.of(CARTE, BANCONTACT) : moyens;
    }

    public boolean estConfigure() {
        return cleSecrete != null && !cleSecrete.isBlank();
    }
}
