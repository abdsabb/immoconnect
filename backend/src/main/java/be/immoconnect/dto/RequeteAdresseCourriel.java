package be.immoconnect.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Une adresse e-mail seule : mot de passe oublié, renvoi du lien d'activation. */
public record RequeteAdresseCourriel(@NotBlank @Email @Size(max = 190) String email) {
}
