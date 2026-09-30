package be.immoconnect.dto;

import be.immoconnect.security.MotDePasseRobuste;
import jakarta.validation.constraints.NotBlank;

/** Nouveau mot de passe, accompagné du jeton reçu par e-mail (cas M10). */
public record RequeteReinitialisation(
        @NotBlank String jeton,
        @NotBlank @MotDePasseRobuste String nouveauMotDePasse) {
}
