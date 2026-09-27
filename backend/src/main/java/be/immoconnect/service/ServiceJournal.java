package be.immoconnect.service;

import be.immoconnect.api.admin.TraceAudit;
import be.immoconnect.depot.JournalAuditRepository;
import be.immoconnect.entite.JournalAudit;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cas d'utilisation « Consulter les rapports d'audit d'activité » (A4, contrainte de l'épreuve).
 * Le journal se lit et se filtre ; aucune opération ne permet de le modifier ni de l'effacer.
 */
@Service
public class ServiceJournal {

    private final JournalAuditRepository journal;
    private final AccesAdministrateur acces;

    public ServiceJournal(JournalAuditRepository journal, AccesAdministrateur acces) {
        this.journal = journal;
        this.acces = acces;
    }

    @Transactional(readOnly = true)
    public Page<TraceAudit> consulter(Integer administrateurId, String action, Integer utilisateurId, LocalDate du, LocalDate au,
                                      Pageable pagination) {
        acces.exiger(administrateurId, AccesAdministrateur.GESTIONNAIRE);
        if (du != null && au != null && au.isBefore(du)) {
            throw new DonneeInvalideException("au", "la fin de la période précède son début");
        }
        Specification<JournalAudit> filtre = Specification.allOf(
                (racine, requete, cb) -> action == null || action.isBlank() ? null : cb.equal(racine.get("action"), action),
                (racine, requete, cb) -> utilisateurId == null ? null : cb.equal(racine.get("utilisateur").get("id"), utilisateurId),
                (racine, requete, cb) -> du == null ? null : cb.greaterThanOrEqualTo(racine.get("horodatage"), du.atStartOfDay()),
                (racine, requete, cb) -> au == null ? null : cb.lessThanOrEqualTo(racine.get("horodatage"), au.atTime(LocalTime.MAX)));
        return journal.findAll(filtre, pagination).map(TraceAudit::depuis);
    }

    @Transactional(readOnly = true)
    public List<String> actions(Integer administrateurId) {
        acces.exiger(administrateurId, AccesAdministrateur.GESTIONNAIRE);
        return journal.actions();
    }
}
