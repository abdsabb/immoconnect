package be.immoconnect.api.rendezvous;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Demande de rendez-vous de visite (cas M4). Le type du créneau et son prix ne sont pas transmis :
 * ils sont déduits de la date par le serveur. {@code paymentIntentId} n'accompagne qu'un créneau premium.
 */
public record RequeteRendezVous(
        @NotNull(message = "le bien est obligatoire") Integer bienId,
        @NotNull(message = "la date de la visite est obligatoire")
        @Future(message = "la date de la visite doit être future") LocalDateTime dateHeure,
        @Size(max = 255, message = "le motif ne peut dépasser 255 caractères") String motif,
        @Pattern(regexp = "^pi_[A-Za-z0-9_]{1,117}$", message = "référence de paiement invalide") String paymentIntentId) {
}
