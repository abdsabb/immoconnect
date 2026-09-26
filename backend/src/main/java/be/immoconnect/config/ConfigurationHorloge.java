package be.immoconnect.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Horloge de l'application, fixée sur le fuseau de l'agence : un créneau de 18 h 30 est à 18 h 30
 * heure de Bruxelles, quel que soit le fuseau du serveur (un conteneur Docker tourne en UTC).
 * L'injecter plutôt qu'appeler LocalDateTime.now() rend aussi les règles de date testables.
 */
@Configuration
public class ConfigurationHorloge {

    public static final ZoneId FUSEAU_AGENCE = ZoneId.of("Europe/Brussels");

    @Bean
    Clock horloge() {
        return Clock.system(FUSEAU_AGENCE);
    }
}
