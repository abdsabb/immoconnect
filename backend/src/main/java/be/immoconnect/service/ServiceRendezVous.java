package be.immoconnect.service;

import be.immoconnect.api.rendezvous.Creneau;
import be.immoconnect.api.rendezvous.RendezVousResume;
import be.immoconnect.api.rendezvous.RequeteRendezVous;
import be.immoconnect.depot.AgentImmobilierRepository;
import be.immoconnect.depot.BienRepository;
import be.immoconnect.depot.MembreRepository;
import be.immoconnect.depot.RendezVousRepository;
import be.immoconnect.entite.Bien;
import be.immoconnect.entite.Membre;
import be.immoconnect.entite.RendezVous;
import be.immoconnect.entite.StatutBien;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cas d'utilisation « Prendre rendez-vous pour visiter un bien » (M4) et « Gérer les demandes de
 * rendez-vous » (AG5) — le scénario validé dans l'analyse UML (version 3/3).
 */
@Service
public class ServiceRendezVous {

    /** Période affichée par défaut, et période maximale qu'une requête peut demander. */
    private static final int PERIODE_PAR_DEFAUT_JOURS = 14;
    private static final int PERIODE_MAX_JOURS = 31;

    private final RendezVousRepository rendezVous;
    private final BienRepository biens;
    private final MembreRepository membres;
    private final AgentImmobilierRepository agents;
    private final GrilleCreneaux grille;
    private final ServiceAudit audit;
    private final Clock horloge;

    public ServiceRendezVous(RendezVousRepository rendezVous, BienRepository biens, MembreRepository membres,
                             AgentImmobilierRepository agents, GrilleCreneaux grille, ServiceAudit audit, Clock horloge) {
        this.rendezVous = rendezVous;
        this.biens = biens;
        this.membres = membres;
        this.agents = agents;
        this.grille = grille;
        this.audit = audit;
        this.horloge = horloge;
    }

    /** Créneaux de la grille encore libres dans l'agenda de l'agent responsable du bien. */
    @Transactional(readOnly = true)
    public List<Creneau> creneauxLibres(Integer bienId, LocalDate du, LocalDate au) {
        Bien bien = bienVisitable(bienId);
        LocalDate debut = du != null ? du : grille.aujourdHui();
        LocalDate fin = au != null ? au : debut.plusDays(PERIODE_PAR_DEFAUT_JOURS - 1);
        if (fin.isBefore(debut)) {
            throw new DonneeInvalideException("au", "la fin de la période précède son début");
        }
        if (fin.isAfter(debut.plusDays(PERIODE_MAX_JOURS - 1))) {
            throw new DonneeInvalideException("au", "la période ne peut dépasser " + PERIODE_MAX_JOURS + " jours");
        }
        Set<LocalDateTime> occupes = new HashSet<>(rendezVous.creneauxOccupes(bien.getAgent().getId(),
                RendezVousRepository.ACTIFS, debut.atStartOfDay(), fin.atTime(LocalTime.MAX)));
        return grille.entre(debut, fin).stream()
                .filter(dateHeure -> !occupes.contains(dateHeure))
                .map(grille::creneau)
                .toList();
    }

    /** Les visites du membre connecté, ou l'agenda de l'agent connecté, de la plus récente à la plus ancienne. */
    @Transactional(readOnly = true)
    public List<RendezVousResume> lister(Integer utilisateurId, String role) {
        return switch (role) {
            case "membre" -> rendezVous.findByMembreIdOrderByDateHeureDesc(utilisateurId).stream()
                    .map(RendezVousResume::pourMembre).toList();
            case "agent" -> rendezVous.findByAgentIdOrderByDateHeureDesc(utilisateurId).stream()
                    .map(RendezVousResume::pourAgent).toList();
            default -> throw new OperationInterditeException("Les rendez-vous sont réservés aux membres et aux agents");
        };
    }

    /**
     * Réserve un créneau standard : le rendez-vous naît au statut « demande » et attend la
     * confirmation de l'agent (scénario alternatif A1).
     * <p>
     * Isolation READ COMMITTED : avec l'isolation par défaut de MySQL (REPEATABLE READ), la transaction
     * qui a attendu le verrou de l'agenda relirait son instantané de départ et ne verrait pas le
     * rendez-vous que la transaction précédente vient de valider — le créneau serait attribué deux fois.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RendezVousResume reserver(Integer membreId, RequeteRendezVous requete, String ip) {
        Membre membre = membres.findById(membreId)
                .orElseThrow(() -> new OperationInterditeException("Seul un membre peut prendre rendez-vous"));
        Bien bien = bienVisitable(requete.bienId());
        if (!grille.estReservable(requete.dateHeure())) {
            throw new DonneeInvalideException("dateHeure", "ce créneau n'existe pas dans la grille des visites");
        }
        if (grille.estPremium(requete.dateHeure())) {
            throw new DonneeInvalideException("paymentIntentId", "un créneau premium doit être payé avant d'être réservé");
        }
        RendezVous rdv = creer(membre, bien, requete.dateHeure(), requete.motif());
        audit.enregistrer(membre, "reservation_rendez_vous", "rendez_vous#" + rdv.getId(), ip);
        return RendezVousResume.pourMembre(rdv);
    }

    /** L'agent accepte la demande : demande -> confirme. Seul l'agent du rendez-vous peut le confirmer. */
    @Transactional
    public RendezVousResume confirmer(Integer id, Integer agentId, String ip) {
        RendezVous rdv = charger(id);
        exigerAgentDuRendezVous(rdv, agentId);
        rdv.confirmer(maintenant());
        audit.enregistrer(rdv.getAgent(), "confirmation_rendez_vous", "rendez_vous#" + id, ip);
        return RendezVousResume.pourAgent(rdv);
    }

    /** Annulation par le membre qui a réservé ou par l'agent qui reçoit : demande/confirme -> annule. */
    @Transactional
    public RendezVousResume annuler(Integer id, Integer utilisateurId, String ip) {
        RendezVous rdv = charger(id);
        boolean parLAgent = rdv.getAgent().getId().equals(utilisateurId);
        if (!parLAgent && !rdv.getMembre().getId().equals(utilisateurId)) {
            throw new OperationInterditeException("Ce rendez-vous ne vous appartient pas");
        }
        rdv.annuler(maintenant());
        audit.enregistrer(parLAgent ? rdv.getAgent() : rdv.getMembre(), "annulation_rendez_vous", "rendez_vous#" + id, ip);
        return parLAgent ? RendezVousResume.pourAgent(rdv) : RendezVousResume.pourMembre(rdv);
    }

    /** Après la visite, l'agent signale qu'elle a eu lieu : confirme -> honore (RA2). */
    @Transactional
    public RendezVousResume honorer(Integer id, Integer agentId, String ip) {
        RendezVous rdv = charger(id);
        exigerAgentDuRendezVous(rdv, agentId);
        rdv.honorer(maintenant());
        audit.enregistrer(rdv.getAgent(), "visite_honoree", "rendez_vous#" + id, ip);
        return RendezVousResume.pourAgent(rdv);
    }

    /**
     * Inscrit le rendez-vous dans l'agenda de l'agent. L'agenda est verrouillé le temps de la
     * transaction : de deux réservations simultanées du même créneau, la seconde est refusée (A2).
     */
    private RendezVous creer(Membre membre, Bien bien, LocalDateTime dateHeure, String motif) {
        Integer agentId = bien.getAgent().getId();
        agents.verrouiller(agentId).orElseThrow(() -> new RessourceIntrouvableException("Agent", agentId));
        if (rendezVous.existsByAgentIdAndDateHeureAndStatutIn(agentId, dateHeure, RendezVousRepository.ACTIFS)) {
            throw new CreneauIndisponibleException("Ce créneau vient d'être réservé par un autre membre.");
        }
        if (rendezVous.existsByMembreIdAndDateHeureAndStatutIn(membre.getId(), dateHeure, RendezVousRepository.ACTIFS)) {
            throw new CreneauIndisponibleException("Vous avez déjà une visite prévue à cette date.");
        }
        String motifNettoye = motif == null || motif.isBlank() ? null : motif.trim();
        return rendezVous.save(new RendezVous(membre, bien, dateHeure, motifNettoye));
    }

    /** RA5 : un bien archivé est introuvable ; sous option, vendu ou loué, il n'accepte plus de visite (E3). */
    private Bien bienVisitable(Integer bienId) {
        Bien bien = biens.findById(bienId)
                .filter(b -> b.getStatut() != StatutBien.archive)
                .orElseThrow(() -> new RessourceIntrouvableException("Bien", bienId));
        if (!bien.estDisponible()) {
            throw new BienIndisponibleException(bienId);
        }
        return bien;
    }

    private RendezVous charger(Integer id) {
        return rendezVous.findWithDetailsById(id).orElseThrow(() -> new RessourceIntrouvableException("Rendez-vous", id));
    }

    /** Contrôle de propriété côté serveur : le rôle agent ne suffit pas, il faut être l'agent de CE rendez-vous. */
    private static void exigerAgentDuRendezVous(RendezVous rdv, Integer agentId) {
        if (!rdv.getAgent().getId().equals(agentId)) {
            throw new OperationInterditeException("Ce rendez-vous appartient à l'agenda d'un autre agent");
        }
    }

    private LocalDateTime maintenant() {
        return LocalDateTime.now(horloge);
    }
}
