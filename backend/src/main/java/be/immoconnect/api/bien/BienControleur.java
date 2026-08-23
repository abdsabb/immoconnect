package be.immoconnect.api.bien;

import be.immoconnect.api.PageReponse;
import be.immoconnect.entite.StatutBien;
import be.immoconnect.service.BienService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints publics du catalogue (livrable 15, §6.2) : GET /biens et GET /biens/{id}. */
@RestController
@RequestMapping("/api/v1/biens")
@Tag(name = "Biens", description = "Catalogue des biens immobiliers (accès public)")
public class BienControleur {

    private static final int TAILLE_MAX = 100;
    private static final Set<String> TRIS_AUTORISES = Set.of("prix", "superficie", "publieLe", "nbChambres", "ville");

    private final BienService service;

    public BienControleur(BienService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Recherche multicritères paginée",
            description = "Filtre par ville, catégorie, fourchette de prix, chambres et superficie. "
                    + "Pagination : page (défaut 0), taille (défaut 20, max 100), tri (ex. prix,asc).")
    public PageReponse<BienResume> rechercher(
            @RequestParam(required = false) String ville,
            @RequestParam(required = false) Integer categorieId,
            @RequestParam(required = false) BigDecimal prixMin,
            @RequestParam(required = false) BigDecimal prixMax,
            @RequestParam(required = false) Integer chambresMin,
            @RequestParam(required = false) BigDecimal superficieMin,
            @RequestParam(required = false) StatutBien statut,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int taille,
            @Parameter(description = "champ,sens — ex. prix,asc ou publieLe,desc")
            @RequestParam(defaultValue = "publieLe,desc") String tri) {
        var criteres = new CritereRechercheBien(ville, categorieId, prixMin, prixMax, chambresMin, superficieMin, statut);
        var pagination = PageRequest.of(Math.max(page, 0), Math.clamp(taille, 1, TAILLE_MAX), tri(tri));
        return PageReponse.depuis(service.rechercher(criteres, pagination));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail complet d'un bien", description = "Photos ordonnées, agent responsable et localisation approximative.")
    public BienDetail detail(@PathVariable Integer id) {
        return service.detail(id);
    }

    /** « prix,asc » -> Sort ; un champ inconnu est refusé (400) pour ne jamais exposer la structure interne. */
    private static Sort tri(String tri) {
        String[] parties = tri.split(",");
        String champ = parties[0].trim();
        if (!TRIS_AUTORISES.contains(champ)) {
            throw new IllegalArgumentException("Paramètre de tri inconnu : " + champ);
        }
        Sort.Direction sens = parties.length > 1 && "desc".equalsIgnoreCase(parties[1].trim())
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(sens, champ);
    }
}
