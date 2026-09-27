package be.immoconnect.api.admin;

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

    /** Biens disponibles et prix moyen par commune, les communes les plus fournies en premier. */
    public record Commune(String ville, long biensDisponibles, BigDecimal prixMoyen) {
    }
}
