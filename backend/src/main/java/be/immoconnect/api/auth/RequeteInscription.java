package be.immoconnect.api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Formulaire d'inscription (cas V7) — règles de validation miroir du dictionnaire de données. */
public record RequeteInscription(
        @NotBlank @Size(max = 80) String nom,
        @NotBlank @Size(max = 80) String prenom,
        @NotBlank @Email @Size(max = 190) String email,
        @NotBlank @Size(min = 8, max = 72, message = "le mot de passe doit compter au moins 8 caractères") String motDePasse,
        @Pattern(regexp = "^$|^\\+?[0-9 ]{8,20}$", message = "numéro de téléphone invalide") String telephone,
        @Pattern(regexp = "^(fr|nl|en)?$", message = "langue inconnue") String langue) {
}
