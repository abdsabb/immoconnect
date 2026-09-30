package be.immoconnect.dto;

import be.immoconnect.entities.TypeOffre;
import java.math.BigDecimal;

/** Ventes et locations ne se mélangent pas : la moyenne d'un prix et d'un loyer n'aurait aucun sens. */
public record StatistiqueMarche(String commune, String categorie, TypeOffre typeOffre, long nbAnnonces,
                                BigDecimal prixMoyen, BigDecimal prixMedianM2) {
}
