package be.immoconnect.dto;

import be.immoconnect.entities.StatutBien;
import be.immoconnect.entities.TypeOffre;
import java.math.BigDecimal;

/** Critères de la recherche avancée (cas V2) : tous optionnels, combinables. */
public record CritereRechercheBien(
        TypeOffre typeOffre,
        String ville,
        Integer categorieId,
        BigDecimal prixMin,
        BigDecimal prixMax,
        Integer chambresMin,
        BigDecimal superficieMin,
        StatutBien statut) {

    /** Critères de la vitrine : les biens disponibles seulement (accueil, flux des nouveautés). */
    public static CritereRechercheBien disponibles() {
        return new CritereRechercheBien(null, null, null, null, null, null, null, StatutBien.disponible);
    }
}
