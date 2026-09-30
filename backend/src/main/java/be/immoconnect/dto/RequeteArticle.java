package be.immoconnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Article rédigé par un administrateur (cas A2). */
public record RequeteArticle(
        @NotBlank(message = "le titre est obligatoire")
        @Size(max = 150, message = "le titre ne peut dépasser 150 caractères") String titre,
        @NotBlank(message = "le contenu est obligatoire")
        // La colonne TEXT contient 65 535 octets : 20 000 caractères y tiennent même accentués
        @Size(max = 20000, message = "le contenu ne peut dépasser 20 000 caractères") String contenu,
        @NotNull(message = "la catégorie est obligatoire") Integer categorieId) {
}
