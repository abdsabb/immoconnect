package be.immoconnect.paiement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import tools.jackson.databind.ObjectMapper;

/**
 * Choix du prestataire de paiement au démarrage : Stripe dès que sa clé secrète est fournie,
 * le prestataire simulé sinon. En production, l'absence de clé arrête le démarrage : mieux vaut
 * une application qui ne démarre pas qu'une application qui encaisse des paiements fictifs.
 */
@Configuration
@EnableConfigurationProperties(ProprietesStripe.class)
public class ConfigurationPaiement {

    private static final Logger journal = LoggerFactory.getLogger(ConfigurationPaiement.class);

    @Bean
    PasserellePaiement passerellePaiement(ProprietesStripe proprietes, ObjectMapper json, Environment environnement) {
        if (proprietes.estConfigure()) {
            journal.info("Paiements : Stripe");
            return new PasserelleStripe(proprietes, json);
        }
        if (environnement.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException("STRIPE_SECRET_KEY est obligatoire en production");
        }
        journal.warn("Paiements : mode simulation (aucune clé Stripe fournie)");
        return new PasserelleSimulee();
    }
}
