package be.immoconnect.dto;

import jakarta.validation.constraints.NotBlank;

/** Un jeton reçu par e-mail : activation d'un compte. */
public record RequeteJeton(@NotBlank String jeton) {
}
