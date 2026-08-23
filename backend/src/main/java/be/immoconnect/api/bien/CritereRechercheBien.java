package be.immoconnect.api.bien;

import be.immoconnect.entite.StatutBien;
import java.math.BigDecimal;

/** Critères de la recherche avancée (cas V2) : tous optionnels, combinables. */
public record CritereRechercheBien(
        String ville,
        Integer categorieId,
        BigDecimal prixMin,
        BigDecimal prixMax,
        Integer chambresMin,
        BigDecimal superficieMin,
        StatutBien statut) {

    /** Par défaut, la recherche publique ne renvoie que les biens disponibles (RA5). */
    public StatutBien statutEffectif() {
        return statut == null ? StatutBien.disponible : statut;
    }
}
