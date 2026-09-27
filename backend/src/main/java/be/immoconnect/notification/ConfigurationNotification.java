package be.immoconnect.notification;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Les notifications partent en tâche de fond : un serveur SMTP lent ne ralentit jamais une réservation. */
@Configuration
@EnableAsync
public class ConfigurationNotification {
}
