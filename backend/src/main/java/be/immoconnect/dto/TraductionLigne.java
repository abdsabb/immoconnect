package be.immoconnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

/**
 * Un texte d'interface dans toutes ses langues (cas A6) : la clé et, par code de langue, sa valeur.
 * Exemple : « accueil.titre » → { fr: …, nl: …, en: … }.
 */
public record TraductionLigne(String cle, Map<String, String> valeurs) {

    /** Valeurs à enregistrer pour une clé ; une langue absente de la requête n'est pas modifiée. */
    public record Requete(Map<@NotBlank String, @NotBlank(message = "une traduction ne peut être vide")
            @Size(max = 2000, message = "une traduction ne peut dépasser 2000 caractères") String> valeurs) {
    }
}
