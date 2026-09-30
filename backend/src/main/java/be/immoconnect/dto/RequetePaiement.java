package be.immoconnect.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/** Créneau premium que le membre s'apprête à payer ; le montant est fixé par le serveur. */
public record RequetePaiement(
        @NotNull(message = "le bien est obligatoire") Integer bienId,
        @NotNull(message = "la date de la visite est obligatoire")
        @Future(message = "la date de la visite doit être future") LocalDateTime dateHeure,
        @Size(max = 255, message = "le motif ne peut dépasser 255 caractères") String motif) {
}
