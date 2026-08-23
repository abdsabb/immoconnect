package be.immoconnect.api.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Changement de mot de passe : l'ancien est exigé pour prouver la possession du compte. */
public record RequeteChangementMotDePasse(
        @NotBlank String ancienMotDePasse,
        @NotBlank @Size(min = 8, max = 72, message = "le mot de passe doit compter au moins 8 caractères") String nouveauMotDePasse) {
}
