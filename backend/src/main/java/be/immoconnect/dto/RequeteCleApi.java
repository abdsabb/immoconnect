package be.immoconnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Demande de clé API : à qui, ou pour quel usage, la clé est délivrée. */
public record RequeteCleApi(
        @NotBlank(message = "le libellé est obligatoire")
        @Size(max = 80, message = "le libellé ne peut dépasser 80 caractères") String libelle) {
}
