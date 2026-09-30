package be.immoconnect.dto;

import jakarta.validation.constraints.NotBlank;

/** Identifiants de connexion (cas M9). */
public record RequeteConnexion(@NotBlank String email, @NotBlank String motDePasse) {
}
