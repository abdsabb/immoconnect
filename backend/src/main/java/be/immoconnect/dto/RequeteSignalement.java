package be.immoconnect.dto;

import be.immoconnect.entities.Signalement;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Signalement d'un contenu par un utilisateur connecté (règlement sur les services numériques). */
public record RequeteSignalement(
        @NotNull(message = "le type de contenu est obligatoire") Signalement.TypeContenu typeContenu,
        @NotNull(message = "le contenu signalé est obligatoire") Integer contenuId,
        @NotNull(message = "le motif est obligatoire") Signalement.Motif motif,
        @NotBlank(message = "expliquez le problème en quelques mots")
        @Size(max = 1000, message = "la description ne peut dépasser 1 000 caractères") String description) {
}
