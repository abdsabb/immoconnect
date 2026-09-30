package be.immoconnect.controllers;

import be.immoconnect.services.ServiceTraductions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dictionnaire d'interface par langue (livrable 15, §6.7) : GET /api/v1/traductions/{code}.
 * Contrainte TFE du multilinguisme : les textes dynamiques sont servis par l'API depuis les
 * tables langue et traduction, gérées par l'administrateur (cas A6).
 */
@RestController
@RequestMapping("/api/v1/traductions")
@Tag(name = "Traductions", description = "Textes d'interface FR / NL / EN (accès public)")
public class TraductionControleur {

    private final ServiceTraductions service;

    public TraductionControleur(ServiceTraductions service) {
        this.service = service;
    }

    @GetMapping("/{code}")
    @Operation(summary = "Dictionnaire d'une langue", description = "Renvoie les couples clé/valeur de la langue (fr, nl ou en).")
    public Map<String, String> dictionnaire(@PathVariable String code) {
        return service.dictionnaire(code);
    }
}
