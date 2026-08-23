package be.immoconnect.service;

import be.immoconnect.depot.JournalAuditRepository;
import be.immoconnect.entite.JournalAudit;
import be.immoconnect.entite.Utilisateur;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Journal d'audit (règle RA13, livrable 16 §7) : toute action sensible — connexion, inscription,
 * création, modification, suppression — laisse une trace horodatée avec l'adresse IP.
 */
@Service
public class ServiceAudit {

    private final JournalAuditRepository journal;

    public ServiceAudit(JournalAuditRepository journal) {
        this.journal = journal;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void enregistrer(Utilisateur utilisateur, String action, String entite, String ip) {
        journal.save(new JournalAudit(utilisateur, action, entite, ip == null ? "inconnue" : ip));
    }
}
