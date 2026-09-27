package be.immoconnect.api.opendata;

import be.immoconnect.service.ServiceOpenData;
import be.immoconnect.service.ServiceOpenData.BienOuvert;
import be.immoconnect.service.ServiceOpenData.StatistiqueMarche;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Volet Open Data (livrable 15, §7) : deux jeux de données sous licence CC BY 4.0, sans aucune donnée
 * personnelle, accessibles avec une clé API (en-tête X-API-Key) délivrée par l'administrateur.
 */
@RestController
@RequestMapping("/api/v1/open-data")
@SecurityRequirement(name = "cleApi")
@Tag(name = "Open Data", description = "Annonces publiées et statistiques du marché, anonymisées (CC BY 4.0)")
public class OpenDataControleur {

    private static final MediaType CSV = new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8);
    private static final String LICENCE = "<https://creativecommons.org/licenses/by/4.0/>; rel=\"license\"";

    private final ServiceOpenData service;

    public OpenDataControleur(ServiceOpenData service) {
        this.service = service;
    }

    @GetMapping(path = "/biens", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Annonces publiées, anonymisées", description = "Biens au statut « disponible » : ni adresse, ni agent, "
            + "position arrondie. JSON par défaut ; CSV avec l'en-tête Accept: text/csv.")
    public ResponseEntity<List<BienOuvert>> biens() {
        return ResponseEntity.ok().header("Link", LICENCE).body(service.biens());
    }

    @GetMapping(path = "/biens", produces = "text/csv")
    @Operation(summary = "Annonces publiées, au format CSV", description = "Séparateur point-virgule, encodage UTF-8, pour les tableurs.")
    public ResponseEntity<String> biensCsv() {
        return ResponseEntity.ok().header("Link", LICENCE)
                .header("Content-Disposition", "attachment; filename=\"immoconnect-biens.csv\"")
                .contentType(CSV).body(service.biensCsv());
    }

    @GetMapping(path = "/statistiques", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Statistiques du marché par commune et par catégorie",
            description = "Nombre d'annonces, prix moyen et prix médian au m², calculés sur les annonces disponibles.")
    public ResponseEntity<List<StatistiqueMarche>> statistiques() {
        return ResponseEntity.ok().header("Link", LICENCE).body(service.statistiques());
    }
}
