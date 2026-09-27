package be.immoconnect.api.paiement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Carte saisie dans le formulaire du mode simulation (numéros de test uniquement). */
public record RequetePaiementSimule(
        @NotBlank String clientSecret,
        @NotBlank @Pattern(regexp = "^[0-9 ]{16,19}$", message = "numéro de carte invalide") String numeroCarte) {
}
