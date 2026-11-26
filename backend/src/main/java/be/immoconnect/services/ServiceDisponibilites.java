package be.immoconnect.services;

import be.immoconnect.dto.DisponibiliteVue;
import be.immoconnect.dto.RequeteDisponibilite;
import be.immoconnect.dto.RetraitDisponibilites;
import be.immoconnect.entities.AgentImmobilier;
import be.immoconnect.entities.Disponibilite;
import be.immoconnect.exceptions.DonneeInvalideException;
import be.immoconnect.exceptions.OperationInterditeException;
import be.immoconnect.exceptions.RessourceIntrouvableException;
import be.immoconnect.exceptions.VisitesPrevuesException;
import be.immoconnect.repositories.AgentImmobilierRepository;
import be.immoconnect.repositories.DisponibiliteRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cas d'utilisation « Gérer ses disponibilités » (AG7). L'agent ouvre dans son agenda les plages pendant
 * lesquelles il reçoit en visite : les membres ne se voient proposer que les créneaux de ces plages (RA15).
 * S'il n'est plus disponible — maladie, congé, empêchement —, il retire ses plages ; les visites déjà prévues
 * sont alors annulées, les membres prévenus par e-mail et les créneaux payés remboursés (RA16).
 * <p>
 * Ouverture et retrait verrouillent l'agenda de l'agent, comme une réservation : un membre ne peut pas
 * réserver un créneau pendant que sa plage est retirée.
 */
@Service
public class ServiceDisponibilites {

    /** L'agenda de l'agent remonte une semaine en arrière par défaut. */
    private static final int JOURS_PASSES_PAR_DEFAUT = 7;
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DisponibiliteRepository disponibilites;
    private final AgentImmobilierRepository agents;
    private final ServiceRendezVous rendezVous;
    private final GrilleCreneaux grille;
    private final ServiceAudit audit;
    private final Clock horloge;

    public ServiceDisponibilites(DisponibiliteRepository disponibilites, AgentImmobilierRepository agents,
                                 ServiceRendezVous rendezVous, GrilleCreneaux grille, ServiceAudit audit, Clock horloge) {
        this.disponibilites = disponibilites;
        this.agents = agents;
        this.rendezVous = rendezVous;
        this.grille = grille;
        this.audit = audit;
        this.horloge = horloge;
    }

    /** Les plages de l'agent sur une période, bornes incluses ; par défaut de la semaine passée à l'horizon de réservation. */
    @Transactional(readOnly = true)
    public List<DisponibiliteVue> lister(Integer agentId, LocalDate du, LocalDate au) {
        LocalDate debut = du != null ? du : grille.aujourdHui().minusDays(JOURS_PASSES_PAR_DEFAUT);
        LocalDate fin = au != null ? au : grille.aujourdHui().plusDays(GrilleCreneaux.HORIZON_JOURS);
        return disponibilites.dansLaPeriode(agentId, debut.atStartOfDay(), fin.plusDays(1).atStartOfDay()).stream()
                .map(this::vue)
                .toList();
    }

    /** L'agent ouvre une plage, éventuellement répétée chaque semaine. Deux plages du même agent ne se chevauchent jamais. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<DisponibiliteVue> ouvrir(Integer agentId, RequeteDisponibilite requete, String ip) {
        AgentImmobilier agent = verrouiller(agentId);
        LocalDateTime maintenant = maintenant();
        valider(requete.debut(), requete.fin(), maintenant);

        List<Disponibilite> ouvertes = new ArrayList<>();
        for (int semaine = 0; semaine < requete.repetitions(); semaine++) {
            LocalDateTime debut = requete.debut().plusWeeks(semaine);
            LocalDateTime fin = requete.fin().plusWeeks(semaine);
            if (debut.toLocalDate().isAfter(maintenant.toLocalDate().plusDays(GrilleCreneaux.HORIZON_JOURS))) {
                throw new DonneeInvalideException(semaine == 0 ? "debut" : "semaines", "la plage du " + JOUR.format(debut)
                        + " est trop lointaine : les visites se réservent " + GrilleCreneaux.HORIZON_JOURS + " jours à l'avance au plus");
            }
            if (!disponibilites.dansLaPeriode(agentId, debut, fin).isEmpty()) {
                throw new DonneeInvalideException("debut", "une disponibilité est déjà ouverte le " + JOUR.format(debut) + " à ces heures");
            }
            ouvertes.add(disponibilites.save(new Disponibilite(agent, debut, fin, maintenant)));
        }
        ouvertes.forEach(plage -> audit.enregistrer(agent, "ouverture_disponibilite", "disponibilite#" + plage.getId(), ip));
        return ouvertes.stream().map(this::vue).toList();
    }

    /** L'agent retire une de ses plages à venir. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RetraitDisponibilites retirer(Integer agentId, Integer id, boolean annulerVisites, String ip) {
        AgentImmobilier agent = verrouiller(agentId);
        Disponibilite plage = disponibilites.findById(id).orElseThrow(() -> new RessourceIntrouvableException("Disponibilité", id));
        // Contrôle de propriété côté serveur : le rôle agent ne suffit pas, il faut être l'agent de CETTE plage
        if (!plage.getAgent().getId().equals(agentId)) {
            throw new OperationInterditeException("Cette disponibilité appartient à l'agenda d'un autre agent");
        }
        if (!plage.getFin().isAfter(maintenant())) {
            throw new IllegalStateException("Cette plage est passée : elle ne se retire plus");
        }
        return retirer(agent, List.of(plage), annulerVisites, ip);
    }

    /** L'agent se déclare indisponible du … au … (bornes incluses) : toutes ses plages à venir de la période sont retirées. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RetraitDisponibilites retirerPeriode(Integer agentId, LocalDate du, LocalDate au, boolean annulerVisites, String ip) {
        AgentImmobilier agent = verrouiller(agentId);
        if (au.isBefore(du)) {
            throw new DonneeInvalideException("au", "la fin de la période précède son début");
        }
        LocalDateTime maintenant = maintenant();
        LocalDateTime debut = du.atStartOfDay().isAfter(maintenant) ? du.atStartOfDay() : maintenant;
        return retirer(agent, disponibilites.dansLaPeriode(agentId, debut, au.plusDays(1).atStartOfDay()), annulerVisites, ip);
    }

    /**
     * RA16 : une plage où des visites sont prévues ne se retire qu'avec l'accord explicite de l'agent ; les visites
     * sont alors annulées avant la plage. Si un remboursement échoue, l'exception annule toute la transaction :
     * la plage et les visites restent en place.
     */
    private RetraitDisponibilites retirer(AgentImmobilier agent, List<Disponibilite> plages, boolean annulerVisites, String ip) {
        int prevues = plages.stream()
                .mapToInt(plage -> rendezVous.visitesPrevues(agent.getId(), plage.getDebut(), plage.getFin()).size())
                .sum();
        if (prevues > 0 && !annulerVisites) {
            throw new VisitesPrevuesException(prevues);
        }
        int annulees = 0;
        for (Disponibilite plage : plages) {
            annulees += rendezVous.annulerLesVisitesDeLaPlage(agent.getId(), plage.getDebut(), plage.getFin(), ip);
            audit.enregistrer(agent, "retrait_disponibilite", "disponibilite#" + plage.getId(), ip);
        }
        disponibilites.deleteAll(plages);
        return new RetraitDisponibilites(plages.size(), annulees);
    }

    /** Une plage tient dans une journée, entre l'ouverture et la fermeture de l'agence, et laisse le temps d'une visite. */
    private static void valider(LocalDateTime debut, LocalDateTime fin, LocalDateTime maintenant) {
        if (!debut.isAfter(maintenant)) {
            throw new DonneeInvalideException("debut", "la plage doit être à venir");
        }
        if (!debut.toLocalDate().equals(fin.toLocalDate())) {
            throw new DonneeInvalideException("fin", "une plage tient dans une seule journée");
        }
        if (Duration.between(debut, fin).compareTo(GrilleCreneaux.DUREE_VISITE) < 0) {
            throw new DonneeInvalideException("fin", "une plage dure au moins une heure : le temps d'une visite");
        }
        if (!surLaDemiHeure(debut) || !surLaDemiHeure(fin)) {
            throw new DonneeInvalideException("debut", "les heures se choisissent par demi-heure");
        }
        if (debut.toLocalTime().isBefore(GrilleCreneaux.OUVERTURE) || fin.toLocalTime().isAfter(GrilleCreneaux.FERMETURE)) {
            throw new DonneeInvalideException("debut", "les visites ont lieu entre 8 h et 21 h");
        }
    }

    private static boolean surLaDemiHeure(LocalDateTime instant) {
        return instant.getMinute() % 30 == 0 && instant.getSecond() == 0 && instant.getNano() == 0;
    }

    private AgentImmobilier verrouiller(Integer agentId) {
        return agents.verrouiller(agentId).orElseThrow(() -> new OperationInterditeException("Seul un agent gère des disponibilités"));
    }

    private DisponibiliteVue vue(Disponibilite plage) {
        return DisponibiliteVue.depuis(plage, grille.decouper(plage.getDebut(), plage.getFin()).size());
    }

    private LocalDateTime maintenant() {
        return LocalDateTime.now(horloge);
    }
}
