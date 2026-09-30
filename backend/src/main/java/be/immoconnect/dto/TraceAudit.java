package be.immoconnect.dto;

import be.immoconnect.entities.JournalAudit;
import java.time.LocalDateTime;

/** Ligne du journal d'audit (cas A4) : qui, quoi, sur quoi, quand, depuis quelle adresse. */
public record TraceAudit(Integer id, LocalDateTime horodatage, String action, String entite, String ip,
                         Integer utilisateurId, String utilisateur, String role) {

    public static TraceAudit depuis(JournalAudit trace) {
        return new TraceAudit(trace.getId(), trace.getHorodatage(), trace.getAction(), trace.getEntite(), trace.getIp(),
                trace.getUtilisateur().getId(), trace.getUtilisateur().getNomComplet(), trace.getUtilisateur().getRole());
    }
}
