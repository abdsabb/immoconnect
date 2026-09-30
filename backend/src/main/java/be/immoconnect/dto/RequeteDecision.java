package be.immoconnect.dto;

import be.immoconnect.entities.Signalement;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Décision de l'administrateur sur un signalement : retirer le contenu ou le conserver, avec sa motivation. */
public record RequeteDecision(
        @NotNull(message = "la décision est obligatoire") Signalement.Statut statut,
        @NotBlank(message = "motivez la décision")
        @Size(max = 500, message = "la motivation ne peut dépasser 500 caractères") String decision) {
}
