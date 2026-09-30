package be.immoconnect.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Tableau de bord de l'administrateur (cas A8). */
public record Statistiques(
        Map<String, Long> biensParStatut,
        Map<String, Long> comptesParRole,
        long comptesDesactives,
        Map<String, Long> rendezVousParStatut,
        long visitesAVenir,
        long paiementsReussis,
        BigDecimal revenusPremium,
        BigDecimal montantRembourse,
        long favoris,
        long messages,
        List<Commune> communes) {

    /**
     * Biens disponibles par commune, les communes les plus fournies en premier. Le prix moyen est celui
     * des biens à vendre ; il est absent d'une commune qui ne propose que des locations.
     */
    public record Commune(String ville, long biensDisponibles, BigDecimal prixMoyen) {
    }
}
