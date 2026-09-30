package be.immoconnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Second facteur : le défi reçu de /auth/login et le code à six chiffres reçu par e-mail. */
public record RequeteCode(
        @NotBlank String defi,
        @NotBlank @Pattern(regexp = "^\\s*[0-9]{6}\\s*$", message = "le code compte six chiffres") String code) {
}
