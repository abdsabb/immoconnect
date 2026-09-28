package be.immoconnect.api.annonce;

import be.immoconnect.entite.StatutBien;
import be.immoconnect.entite.TypeOffre;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Formulaire d'une annonce (cas AG1 et AG2) — règles de validation et du domaine du dictionnaire de
 * données, miroir des contraintes CHECK de la table bien. L'agent responsable n'est pas transmis :
 * c'est l'agent connecté. Sans type d'offre, une nouvelle annonce est une vente et une annonce
 * modifiée garde le sien.
 */
public record RequeteBien(
        @NotNull(message = "la catégorie est obligatoire") Integer categorieId,
        @NotBlank(message = "le titre est obligatoire")
        @Size(max = 150, message = "le titre ne peut dépasser 150 caractères") String titre,
        @NotBlank(message = "la description est obligatoire")
        @Size(max = 10000, message = "la description ne peut dépasser 10 000 caractères") String description,
        @NotNull(message = "le prix est obligatoire")
        @DecimalMin(value = "0.01", message = "le prix doit être strictement positif")
        @Digits(integer = 10, fraction = 2, message = "prix invalide") BigDecimal prix,
        @NotNull(message = "la superficie est obligatoire")
        @DecimalMin(value = "0.01", message = "la superficie doit être strictement positive")
        @Digits(integer = 6, fraction = 2, message = "superficie invalide") BigDecimal superficie,
        @NotNull(message = "le nombre de chambres est obligatoire")
        @Min(value = 0, message = "le nombre de chambres ne peut être négatif")
        @Max(value = 20, message = "20 chambres au plus") Integer nbChambres,
        @NotBlank(message = "l'adresse est obligatoire")
        @Size(max = 150, message = "l'adresse ne peut dépasser 150 caractères") String adresse,
        @NotBlank(message = "la ville est obligatoire")
        @Size(max = 80, message = "la ville ne peut dépasser 80 caractères") String ville,
        @NotBlank(message = "le code postal est obligatoire")
        @Pattern(regexp = "^[1-9][0-9]{3}$", message = "code postal belge à 4 chiffres") String codePostal,
        @NotNull(message = "la latitude est obligatoire")
        @DecimalMin(value = "-90", message = "latitude entre -90 et 90")
        @DecimalMax(value = "90", message = "latitude entre -90 et 90") BigDecimal latitude,
        @NotNull(message = "la longitude est obligatoire")
        @DecimalMin(value = "-180", message = "longitude entre -180 et 180")
        @DecimalMax(value = "180", message = "longitude entre -180 et 180") BigDecimal longitude,
        StatutBien statut,
        TypeOffre typeOffre) {
}
