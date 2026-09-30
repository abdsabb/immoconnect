package be.immoconnect.controllers;

import be.immoconnect.entities.TypeOffre;
import be.immoconnect.services.FluxRss;
import be.immoconnect.services.ServiceFlux;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Flux RSS 2.0, publics et sans clé : un lecteur de flux ne sait pas s'authentifier. */
@RestController
@RequestMapping(ServiceFlux.CHEMIN)
@Tag(name = "Flux RSS", description = "Derniers articles et dernières annonces, pour les lecteurs de flux")
public class FluxControleur {

    /** Un lecteur de flux revient souvent : un quart d'heure de cache épargne la base. */
    private static final CacheControl CACHE = CacheControl.maxAge(Duration.ofMinutes(15)).cachePublic();

    private final ServiceFlux service;

    public FluxControleur(ServiceFlux service) {
        this.service = service;
    }

    @GetMapping(value = "/articles", produces = FluxRss.TYPE)
    @Operation(summary = "Flux RSS du blog", description = "Les 20 derniers articles publiés (RA4).")
    public ResponseEntity<String> articles() {
        return reponse(service.articles());
    }

    @GetMapping(value = "/biens", produces = FluxRss.TYPE)
    @Operation(summary = "Flux RSS des annonces",
            description = "Les 20 dernières annonces disponibles, à vendre, à louer ou les deux. Ni adresse exacte ni nom d'agent.")
    public ResponseEntity<String> biens(@RequestParam(required = false) TypeOffre typeOffre) {
        return reponse(service.biens(typeOffre));
    }

    private static ResponseEntity<String> reponse(String flux) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(FluxRss.TYPE)).cacheControl(CACHE).body(flux);
    }
}
