package be.immoconnect.dto;

import be.immoconnect.entities.DemandeContact;
import java.time.LocalDateTime;

/** Demande de contact telle que la lit l'administrateur. */
public record DemandeContactResume(Integer id, String nom, String email, String telephone, String sujet, String message,
                                   LocalDateTime creeLe, boolean traitee, LocalDateTime traiteLe, String traitePar) {

    public static DemandeContactResume depuis(DemandeContact d) {
        return new DemandeContactResume(d.getId(), d.getNom(), d.getEmail(), d.getTelephone(), d.getSujet(), d.getMessage(),
                d.getCreeLe(), d.estTraitee(), d.getTraiteLe(), d.getTraitePar() == null ? null : d.getTraitePar().getNomComplet());
    }
}
