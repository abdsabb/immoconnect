package be.immoconnect.dto;

import be.immoconnect.entities.TypeOffre;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Annonce du jeu de données ouvert. L'anonymisation tient à sa forme : l'objet n'a pas de champ pour
 * l'adresse, l'agent ou un identifiant. Pour une location, le prix est le loyer mensuel.
 */
public record BienOuvert(TypeOffre typeOffre, String categorie, String ville, String codePostal, BigDecimal prix,
                         BigDecimal superficie, Integer nbChambres, BigDecimal latitude, BigDecimal longitude,
                         LocalDate publieLe) {
}
