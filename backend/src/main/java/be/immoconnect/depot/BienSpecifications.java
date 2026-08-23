package be.immoconnect.depot;

import be.immoconnect.entite.Bien;
import be.immoconnect.entite.StatutBien;
import java.math.BigDecimal;
import org.springframework.data.jpa.domain.Specification;

/** Critères de la recherche multicritères (cas « Rechercher un bien »), composables entre eux. */
public final class BienSpecifications {

    private BienSpecifications() {
    }

    public static Specification<Bien> statut(StatutBien statut) {
        return (racine, requete, cb) -> statut == null ? null : cb.equal(racine.get("statut"), statut);
    }

    public static Specification<Bien> ville(String ville) {
        return (racine, requete, cb) -> (ville == null || ville.isBlank()) ? null
                : cb.like(cb.lower(racine.get("ville")), ville.trim().toLowerCase() + "%");
    }

    public static Specification<Bien> categorie(Integer categorieId) {
        return (racine, requete, cb) -> categorieId == null ? null : cb.equal(racine.get("categorie").get("id"), categorieId);
    }

    public static Specification<Bien> prixMin(BigDecimal prixMin) {
        return (racine, requete, cb) -> prixMin == null ? null : cb.greaterThanOrEqualTo(racine.get("prix"), prixMin);
    }

    public static Specification<Bien> prixMax(BigDecimal prixMax) {
        return (racine, requete, cb) -> prixMax == null ? null : cb.lessThanOrEqualTo(racine.get("prix"), prixMax);
    }

    public static Specification<Bien> chambresMin(Integer chambres) {
        return (racine, requete, cb) -> chambres == null ? null : cb.greaterThanOrEqualTo(racine.get("nbChambres"), chambres);
    }

    public static Specification<Bien> superficieMin(BigDecimal superficie) {
        return (racine, requete, cb) -> superficie == null ? null : cb.greaterThanOrEqualTo(racine.get("superficie"), superficie);
    }
}
