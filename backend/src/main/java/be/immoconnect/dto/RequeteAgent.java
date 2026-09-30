package be.immoconnect.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Création du compte d'un agent par l'administrateur. L'inscription publique ne crée que des membres :
 * un compte agent donne accès aux données de tiers, il est donc ouvert par l'agence. Le matricule est
 * attribué par le serveur.
 */
public record RequeteAgent(
        @NotBlank(message = "le nom est obligatoire") @Size(max = 80) String nom,
        @NotBlank(message = "le prénom est obligatoire") @Size(max = 80) String prenom,
        @NotBlank(message = "l'adresse e-mail est obligatoire") @Email(message = "adresse e-mail invalide") @Size(max = 190) String email,
        @NotBlank(message = "le téléphone professionnel est obligatoire")
        @Pattern(regexp = "^\\+?[0-9 ]{8,20}$", message = "numéro de téléphone invalide") String telephonePro,
        @Pattern(regexp = "^(fr|nl|en)?$", message = "langue inconnue") String langue,
        @NotBlank(message = "le mot de passe provisoire est obligatoire")
        @Size(min = 8, max = 72, message = "le mot de passe doit compter au moins 8 caractères") String motDePasse) {
}
