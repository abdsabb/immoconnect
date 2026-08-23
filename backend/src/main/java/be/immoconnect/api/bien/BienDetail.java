package be.immoconnect.api.bien;

import be.immoconnect.entite.Bien;
import be.immoconnect.entite.StatutBien;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Fiche complète d'un bien (gabarit « article »). L'adresse exacte n'est volontairement pas
 * exposée publiquement : elle n'est communiquée qu'après prise de rendez-vous (livrables 10 et 16).
 */
public record BienDetail(
        Integer id,
        String titre,
        String description,
        BigDecimal prix,
        BigDecimal superficie,
        Integer nbChambres,
        String ville,
        String codePostal,
        BigDecimal latitude,
        BigDecimal longitude,
        StatutBien statut,
        LocalDate publieLe,
        Categorie categorie,
        Agent agent,
        List<Photo> photos) {

    public record Categorie(Integer id, String nom) {
    }

    public record Agent(Integer id, String nomComplet, String telephonePro) {
    }

    public record Photo(String url, Integer ordre, String legende) {
    }

    public static BienDetail depuis(Bien bien) {
        return new BienDetail(bien.getId(), bien.getTitre(), bien.getDescription(), bien.getPrix(),
                bien.getSuperficie(), bien.getNbChambres(), bien.getVille(), bien.getCodePostal(),
                bien.getLatitude(), bien.getLongitude(), bien.getStatut(), bien.getPublieLe(),
                new Categorie(bien.getCategorie().getId(), bien.getCategorie().getNom()),
                new Agent(bien.getAgent().getId(), bien.getAgent().getNomComplet(), bien.getAgent().getTelephonePro()),
                bien.getPhotos().stream().map(p -> new Photo(p.getUrl(), p.getOrdre(), p.getLegende())).toList());
    }
}
