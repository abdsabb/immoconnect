package be.immoconnect.dto;

import be.immoconnect.security.MotDePasseRobuste;
import jakarta.validation.constraints.NotBlank;

/** Changement de mot de passe : l'ancien est exigé pour prouver la possession du compte. */
public record RequeteChangementMotDePasse(
        @NotBlank String ancienMotDePasse,
        @NotBlank @MotDePasseRobuste String nouveauMotDePasse) {
}
