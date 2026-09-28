package be.immoconnect.api.annonce;

import be.immoconnect.entite.Bien;
import be.immoconnect.entite.StatutBien;
import be.immoconnect.entite.TypeOffre;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Annonce vue par son agent : toutes ses données, adresse exacte comprise, et les indicateurs du
 * tableau de bord (cas AG6). Jamais renvoyée au public.
 */
public record BienGestion(
        Integer id,
        String titre,
        String description,
        TypeOffre typeOffre,
        BigDecimal prix,
        BigDecimal superficie,
        Integer nbChambres,
        String adresse,
        String ville,
        String codePostal,
        BigDecimal latitude,
        BigDecimal longitude,
        StatutBien statut,
        LocalDate publieLe,
        Integer categorieId,
        String categorie,
        List<PhotoGestion> photos,
        Indicateurs indicateurs) {

    public record PhotoGestion(Integer id, String url, Integer ordre, String legende) {
    }

    /** Favoris, demandes de visite en attente et visites confirmées à venir. */
    public record Indicateurs(long favoris, long demandesEnAttente, long visitesAVenir) {

        public static final Indicateurs AUCUN = new Indicateurs(0, 0, 0);
    }

    public static BienGestion depuis(Bien bien, Indicateurs indicateurs) {
        return new BienGestion(bien.getId(), bien.getTitre(), bien.getDescription(), bien.getTypeOffre(), bien.getPrix(),
                bien.getSuperficie(),
                bien.getNbChambres(), bien.getAdresse(), bien.getVille(), bien.getCodePostal(), bien.getLatitude(),
                bien.getLongitude(), bien.getStatut(), bien.getPublieLe(), bien.getCategorie().getId(),
                bien.getCategorie().getNom(),
                bien.getPhotos().stream().map(p -> new PhotoGestion(p.getId(), p.getUrl(), p.getOrdre(), p.getLegende())).toList(),
                indicateurs);
    }
}
