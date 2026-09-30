package be.immoconnect.dto;

import be.immoconnect.entities.Signalement;
import java.time.LocalDateTime;

/** Signalement tel que le voit l'administrateur, avec un aperçu du contenu visé. */
public record SignalementResume(
        Integer id,
        Signalement.TypeContenu typeContenu,
        Integer contenuId,
        String apercu,
        Signalement.Motif motif,
        String description,
        Signalement.Statut statut,
        LocalDateTime creeLe,
        String auteur,
        String auteurRole,
        LocalDateTime traiteLe,
        String traitePar,
        String decision) {

    public static SignalementResume depuis(Signalement s, String apercu) {
        return new SignalementResume(s.getId(), s.getTypeContenu(), s.getContenuId(), apercu, s.getMotif(), s.getDescription(),
                s.getStatut(), s.getCreeLe(), s.getAuteur().getNomComplet(), s.getAuteur().getRole(), s.getTraiteLe(),
                s.getTraitePar() == null ? null : s.getTraitePar().getNomComplet(), s.getDecision());
    }
}
