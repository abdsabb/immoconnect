package be.immoconnect.api.traduction;

import be.immoconnect.depot.LangueRepository;
import be.immoconnect.depot.TraductionRepository;
import be.immoconnect.entite.Traduction;
import be.immoconnect.service.RessourceIntrouvableException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
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

    private final LangueRepository langues;
    private final TraductionRepository traductions;

    public TraductionControleur(LangueRepository langues, TraductionRepository traductions) {
        this.langues = langues;
        this.traductions = traductions;
    }

    @GetMapping("/{code}")
    @Operation(summary = "Dictionnaire d'une langue", description = "Renvoie les couples clé/valeur de la langue (fr, nl ou en).")
    @Transactional(readOnly = true)
    public Map<String, String> dictionnaire(@PathVariable String code) {
        String codeNormalise = code.toLowerCase();
        langues.findByCode(codeNormalise).orElseThrow(() -> new RessourceIntrouvableException("Langue", code));
        return traductions.findByLangueCode(codeNormalise).stream()
                .collect(Collectors.toMap(Traduction::getCle, Traduction::getValeur, (a, b) -> a, TreeMap::new));
    }
}
