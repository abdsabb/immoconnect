package be.immoconnect.dto;

import be.immoconnect.security.MotDePasseRobuste;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Formulaire d'inscription (cas V7) — règles de validation miroir du dictionnaire de données.
 * Les conditions générales doivent être acceptées explicitement ; le consentement aux communications
 * est un choix, refusé par défaut (livrable 18, §3).
 */
public record RequeteInscription(
        @NotBlank @Size(max = 80) String nom,
        @NotBlank @Size(max = 80) String prenom,
        @NotBlank @Email @Size(max = 190) String email,
        @NotBlank @MotDePasseRobuste String motDePasse,
        @Pattern(regexp = "^$|^\\+?[0-9 ]{8,20}$", message = "numéro de téléphone invalide") String telephone,
        @Pattern(regexp = "^(fr|nl|en)?$", message = "langue inconnue") String langue,
        @NotNull(message = "les conditions générales doivent être acceptées")
        @AssertTrue(message = "les conditions générales doivent être acceptées") Boolean cguAcceptees,
        Boolean consentementCommunications) {
}
