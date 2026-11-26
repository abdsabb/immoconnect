package be.immoconnect.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Plage de disponibilité qu'un agent ouvre dans son agenda (cas AG7). {@code semaines} répète la même plage,
 * le même jour à la même heure, pendant ce nombre de semaines : 1 (par défaut) ouvre la plage une seule fois.
 */
public record RequeteDisponibilite(
        @NotNull(message = "le début de la plage est obligatoire") LocalDateTime debut,
        @NotNull(message = "la fin de la plage est obligatoire") LocalDateTime fin,
        @Min(value = 1, message = "une semaine au moins")
        @Max(value = 8, message = "huit semaines au plus") Integer semaines) {

    public int repetitions() {
        return semaines == null ? 1 : semaines;
    }
}
