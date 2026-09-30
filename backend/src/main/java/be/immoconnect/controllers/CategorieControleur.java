package be.immoconnect.controllers;

import be.immoconnect.dto.CategorieResume;
import be.immoconnect.services.ServiceCategories;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Catégories de biens : liste publique, utilisée par la recherche et par le formulaire d'annonce. */
@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Catégories", description = "Catégories de biens immobiliers")
public class CategorieControleur {

    private final ServiceCategories service;

    public CategorieControleur(ServiceCategories service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Catégories de biens, par ordre alphabétique")
    public List<CategorieResume> lister() {
        return service.lister();
    }
}
