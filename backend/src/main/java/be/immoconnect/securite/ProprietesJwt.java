package be.immoconnect.securite;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Paramètres des jetons JWT (immoconnect.jwt.*) : secret HS256 hors du dépôt en production, durée de vie courte. */
@ConfigurationProperties(prefix = "immoconnect.jwt")
public record ProprietesJwt(String secret, long dureeSecondes) {
}
