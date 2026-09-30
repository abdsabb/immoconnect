package be.immoconnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Catégorie de biens (cas A3) : un nom unique et une description facultative. */
public record RequeteCategorie(
        @NotBlank(message = "le nom est obligatoire")
        @Size(max = 60, message = "le nom ne peut dépasser 60 caractères") String nom,
        @Size(max = 1000, message = "la description ne peut dépasser 1000 caractères") String description) {
}
