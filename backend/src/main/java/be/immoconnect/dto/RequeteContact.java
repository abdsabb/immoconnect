package be.immoconnect.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Formulaire de contact. « site » est un champ piège : invisible pour une personne, il n'est rempli que par un
 * robot ; une demande qui le remplit est ignorée sans le dire.
 */
public record RequeteContact(
        @NotBlank(message = "le nom est obligatoire") @Size(max = 100, message = "100 caractères au plus") String nom,
        @NotBlank(message = "l'adresse e-mail est obligatoire") @Email(message = "adresse e-mail invalide")
        @Size(max = 150, message = "150 caractères au plus") String email,
        @Size(max = 30, message = "30 caractères au plus") String telephone,
        @NotBlank(message = "le sujet est obligatoire") @Size(max = 150, message = "150 caractères au plus") String sujet,
        @NotBlank(message = "le message est obligatoire") @Size(max = 3000, message = "3 000 caractères au plus") String message,
        String site) {
}
