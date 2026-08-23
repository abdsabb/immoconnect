package be.immoconnect.api.bien;

import be.immoconnect.entite.Bien;
import be.immoconnect.entite.Photo;
import be.immoconnect.entite.StatutBien;
import java.math.BigDecimal;

/** Carte d'un bien dans la liste de résultats (gabarit « rubrique » du livrable 10). */
public record BienResume(
        Integer id,
        String titre,
        BigDecimal prix,
        BigDecimal superficie,
        Integer nbChambres,
        String ville,
        String codePostal,
        BigDecimal latitude,
        BigDecimal longitude,
        StatutBien statut,
        String categorie,
        AgentResume agent,
        String photoCouverture) {

    public record AgentResume(Integer id, String nomComplet) {
    }

    public static BienResume depuis(Bien bien) {
        String couverture = bien.getPhotos().stream()
                .filter(Photo::estCouverture).map(Photo::getUrl).findFirst()
                .orElseGet(() -> bien.getPhotos().isEmpty() ? null : bien.getPhotos().getFirst().getUrl());
        return new BienResume(bien.getId(), bien.getTitre(), bien.getPrix(), bien.getSuperficie(),
                bien.getNbChambres(), bien.getVille(), bien.getCodePostal(), bien.getLatitude(), bien.getLongitude(),
                bien.getStatut(), bien.getCategorie().getNom(),
                new AgentResume(bien.getAgent().getId(), bien.getAgent().getNomComplet()), couverture);
    }
}
