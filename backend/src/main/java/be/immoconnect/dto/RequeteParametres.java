package be.immoconnect.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.Map;

/** Valeurs à enregistrer, par clé ; les clés inconnues sont refusées par le service. */
public record RequeteParametres(@NotEmpty(message = "aucun paramètre transmis") Map<String, String> valeurs) {
}
