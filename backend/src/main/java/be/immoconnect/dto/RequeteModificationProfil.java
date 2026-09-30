package be.immoconnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Modification du profil (cas M6) : identité, téléphone, langue préférée, et deux choix laissés à
 * l'utilisateur, le double facteur et le consentement aux communications. Un choix absent est inchangé.
 */
public record RequeteModificationProfil(
        @NotBlank @Size(max = 80) String nom,
        @NotBlank @Size(max = 80) String prenom,
        @Pattern(regexp = "^$|^\\+?[0-9 ]{8,20}$", message = "numéro de téléphone invalide") String telephone,
        @Pattern(regexp = "^(fr|nl|en)$", message = "langue inconnue") String langue,
        Boolean doubleFacteur,
        Boolean consentementCommunications) {
}
