package be.immoconnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Message à envoyer : l'expéditeur vient du jeton, jamais du corps de la requête. */
public record RequeteMessage(
        @NotNull(message = "le destinataire est obligatoire") Integer destinataireId,
        @NotBlank(message = "le message est vide")
        @Size(max = 5000, message = "le message ne peut dépasser 5000 caractères") String contenu) {
}
