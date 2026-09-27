package be.immoconnect.config;

import java.util.TimeZone;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Fixe le fuseau horaire de la JVM sur celui de l'agence, avant la création du moindre composant.
 * <p>
 * Les dates de la base sont des heures locales de Bruxelles (DATETIME, sans fuseau). Sur un serveur
 * en UTC — conteneur Docker, intégration continue —, la conversion entre la JVM et la base les
 * décalait de une à deux heures : un rendez-vous de 10 h 30 était enregistré à 12 h 30. L'application
 * ne dépend plus du réglage de la machine qui l'héberge.
 * <p>
 * Déclaré dans META-INF/spring.factories : il s'exécute avant l'ouverture des connexions à la base.
 */
public class FuseauHoraireAgence implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext contexte) {
        TimeZone.setDefault(TimeZone.getTimeZone(ConfigurationHorloge.FUSEAU_AGENCE));
    }
}
