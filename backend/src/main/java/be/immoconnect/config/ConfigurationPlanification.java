package be.immoconnect.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Tâches planifiées : purge des jetons expirés, détection d'intrusion (livrable 16, §6). */
@Configuration
@EnableScheduling
public class ConfigurationPlanification {
}
