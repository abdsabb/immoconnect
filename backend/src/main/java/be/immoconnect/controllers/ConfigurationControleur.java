package be.immoconnect.controllers;

import be.immoconnect.dto.ConfigurationPublique;
import be.immoconnect.security.ProprietesSecurite;
import be.immoconnect.services.ServiceParametres;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Réglages publics du site, lus par l'interface au démarrage. */
@RestController
@RequestMapping("/api/v1/configuration")
@Tag(name = "Configuration", description = "Réglages publics du site")
public class ConfigurationControleur {

    private final ProprietesSecurite securite;
    private final ServiceParametres parametres;
    private final String boiteDeDemonstration;

    public ConfigurationControleur(ProprietesSecurite securite, ServiceParametres parametres,
                                   @Value("${immoconnect.demonstration.boite-courriels:}") String boiteDeDemonstration) {
        this.securite = securite;
        this.parametres = parametres;
        this.boiteDeDemonstration = boiteDeDemonstration;
    }

    @GetMapping
    @Operation(summary = "Réglages publics", description = "Activation par e-mail, double facteur, boîte de démonstration éventuelle, identité de l'agence et langues actives (cas A5).")
    public ConfigurationPublique lire() {
        return new ConfigurationPublique(securite.activationParCourriel(), securite.doubleFacteur(),
                boiteDeDemonstration.isBlank() ? null : boiteDeDemonstration, parametres.site());
    }
}
