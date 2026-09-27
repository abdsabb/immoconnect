package be.immoconnect.api.categorie;

import be.immoconnect.depot.CategorieRepository;
import be.immoconnect.entite.Categorie;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Catégories de biens : liste publique, utilisée par la recherche et par le formulaire d'annonce. */
@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Catégories", description = "Catégories de biens immobiliers")
public class CategorieControleur {

    public record CategorieResume(Integer id, String nom, String description) {

        public static CategorieResume depuis(Categorie categorie) {
            return new CategorieResume(categorie.getId(), categorie.getNom(), categorie.getDescription());
        }
    }

    private final CategorieRepository categories;

    public CategorieControleur(CategorieRepository categories) {
        this.categories = categories;
    }

    @GetMapping
    @Operation(summary = "Catégories de biens, par ordre alphabétique")
    public List<CategorieResume> lister() {
        return categories.findAll(Sort.by("nom")).stream().map(CategorieResume::depuis).toList();
    }
}
