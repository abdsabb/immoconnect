package be.immoconnect.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Documentation OpenAPI 3 générée par springdoc (livrable 15) : Swagger UI sur /swagger-ui.html,
 * spécification sur /v3/api-docs. Deux schémas de sécurité : JWT (utilisateurs) et clé API (consommateurs externes).
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "ImmoConnect API", version = "1.0.0",
                description = "API RESTful de la plateforme ImmoConnect : catalogue des biens, rendez-vous de visite, "
                        + "paiements Stripe, blog, traductions et Open Data. Erreurs au format problem+json (RFC 7807).",
                contact = @Contact(name = "Abdulrahman Sabbagh", url = "https://github.com/abdsabb/immoconnect")),
        servers = {@Server(url = "/", description = "Serveur courant")})
@SecurityScheme(name = "jwt", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT",
        description = "Jeton obtenu via POST /api/v1/auth/login")
@SecurityScheme(name = "cleApi", type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.HEADER, paramName = "X-API-Key",
        description = "Clé délivrée par l'administrateur (Open Data et partenaires)")
public class ConfigurationOpenApi {
}
