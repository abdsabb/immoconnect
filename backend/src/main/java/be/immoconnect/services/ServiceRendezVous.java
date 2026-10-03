package be.immoconnect.services;

import be.immoconnect.dto.Creneau;
import be.immoconnect.dto.RendezVousResume;
import be.immoconnect.dto.ReponseIntention;
import be.immoconnect.dto.RequetePaiement;
import be.immoconnect.dto.RequeteRendezVous;
import be.immoconnect.entities.Bien;
import be.immoconnect.entities.Membre;
import be.immoconnect.entities.Paiement;
import be.immoconnect.entities.RendezVous;
import be.immoconnect.entities.StatutBien;
import be.immoconnect.entities.StatutPaiement;
import be.immoconnect.entities.Utilisateur;
import be.immoconnect.exceptions.BienIndisponibleException;
import be.immoconnect.exceptions.CreneauIndisponibleException;
import be.immoconnect.exceptions.DonneeInvalideException;
import be.immoconnect.exceptions.OperationInterditeException;
import be.immoconnect.exceptions.RessourceIntrouvableException;
import be.immoconnect.notification.EvenementRendezVous;
import be.immoconnect.paiement.EvenementPaiement;
import be.immoconnect.paiement.IntentionPaiement;
import be.immoconnect.paiement.PasserellePaiement;
import be.immoconnect.paiement.PasserellePaiement.IntentionIntrouvableException;
import be.immoconnect.repositories.AgentImmobilierRepository;
import be.immoconnect.repositories.BienRepository;
import be.immoconnect.repositories.MembreRepository;
import be.immoconnect.repositories.PaiementRepository;
import be.immoconnect.repositories.RendezVousRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cas d'utilisation « Prendre rendez-vous pour visiter un bien » (M4), « Payer un créneau premium »
 * (M5) et « Gérer les demandes de rendez-vous » (AG5) — le scénario validé dans l'analyse UML (version 3/3).
 * <p>
 * Les écritures se font en isolation READ COMMITTED : avec l'isolation par défaut de MySQL
 * (REPEATABLE READ), la transaction qui a attendu le verrou de l'agenda relirait son instantané de
 * départ et ne verrait pas le rendez-vous que la transaction précédente vient de valider — le
 * créneau serait attribué deux fois.
 */
@Service
public class ServiceRendezVous {

    private static final Logger journal = LoggerFactory.getLogger(ServiceRendezVous.class);

    /** Période affichée par défaut, et période maximale qu'une requête peut demander. */
    private static final int PERIODE_PAR_DEFAUT_JOURS = 14;
    private static final int PERIODE_MAX_JOURS = 31;

    /** Ce que l'intention de paiement retient du créneau : de quoi créer le rendez-vous depuis le webhook. */
    private static final String META_MEMBRE = "membre_id";
    private static final String META_BIEN = "bien_id";
    private static final String META_DATE = "date_heure";
    private static final String META_MOTIF = "motif";
    private static final String DEVISE = "eur";
    private static final String ORIGINE_WEBHOOK = "webhook-stripe";

    private final RendezVousRepository rendezVous;
    private final PaiementRepository paiements;
    private final BienRepository biens;
    private final MembreRepository membres;
    private final AgentImmobilierRepository agents;
    private final GrilleCreneaux grille;
    private final PasserellePaiement passerelle;
    private final ServiceAudit audit;
    private final ApplicationEventPublisher evenements;
    private final Clock horloge;

    public ServiceRendezVous(RendezVousRepository rendezVous, PaiementRepository paiements, BienRepository biens,
                             MembreRepository membres, AgentImmobilierRepository agents, GrilleCreneaux grille,
                             PasserellePaiement passerelle, ServiceAudit audit, ApplicationEventPublisher evenements,
                             Clock horloge) {
        this.evenements = evenements;
        this.rendezVous = rendezVous;
        this.paiements = paiements;
        this.biens = biens;
        this.membres = membres;
        this.agents = agents;
        this.grille = grille;
        this.passerelle = passerelle;
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
     * Étape 6 du scénario nominal : la demande de paiement du créneau premium est transmise au
     * prestataire. Rien n'est encore écrit en base — si le paiement échoue, aucun rendez-vous n'existe (E1).
     */
    @Transactional(readOnly = true)
    public ReponseIntention preparerPaiement(Integer membreId, RequetePaiement requete) {
        Membre membre = membre(membreId);
        Bien bien = bienVisitable(requete.bienId());
        exigerReservable(requete.dateHeure());
        if (!grille.estPremium(requete.dateHeure())) {
            throw new DonneeInvalideException("dateHeure", "un créneau standard est gratuit : il se réserve sans paiement");
        }
        exigerCreneauLibre(membre, bien, requete.dateHeure());

        Map<String, String> metadonnees = new HashMap<>();
        metadonnees.put(META_MEMBRE, String.valueOf(membreId));
        metadonnees.put(META_BIEN, String.valueOf(bien.getId()));
        metadonnees.put(META_DATE, requete.dateHeure().toString());
        if (requete.motif() != null && !requete.motif().isBlank()) {
            metadonnees.put(META_MOTIF, requete.motif().trim());
        }
        String description = "ImmoConnect — visite premium du bien n° " + bien.getId() + ", " + requete.dateHeure();
        return ReponseIntention.depuis(passerelle.creerIntention(centimes(grille.prix(requete.dateHeure())),
                description, membre.getEmail(), metadonnees));
    }

    /**
     * Réserve un créneau.
     * <ul>
     *   <li>standard : le rendez-vous naît au statut « demande » et attend la confirmation de l'agent (A1) ;</li>
     *   <li>premium : le paiement est vérifié auprès du prestataire, puis le rendez-vous naît « confirmé » (nominal).</li>
     * </ul>
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RendezVousResume reserver(Integer membreId, RequeteRendezVous requete, String ip) {
        Membre membre = membre(membreId);
        if (requete.paymentIntentId() != null) {
            return RendezVousResume.pourMembre(reserverPremium(membre, requete, ip));
        }
        Bien bien = bienVisitable(requete.bienId());
        exigerReservable(requete.dateHeure());
        if (grille.estPremium(requete.dateHeure())) {
            throw new DonneeInvalideException("paymentIntentId", "un créneau premium doit être payé avant d'être réservé");
        }
        verrouillerAgenda(bien);
        exigerCreneauLibre(membre, bien, requete.dateHeure());
        RendezVous rdv = rendezVous.save(new RendezVous(membre, bien, requete.dateHeure(), nettoyer(requete.motif())));
        audit.enregistrer(membre, "creation_rdv", "rendez_vous#" + rdv.getId(), ip);
        publier(rdv, EvenementRendezVous.Type.demande);
        return RendezVousResume.pourMembre(rdv);
    }

    /**
     * Notification du prestataire de paiement. Le paiement réussi crée le rendez-vous s'il n'existe
     * pas encore : le membre qui ferme son navigateur juste après avoir payé obtient quand même sa visite.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void traiterEvenement(EvenementPaiement evenement) {
        switch (evenement.type()) {
            case paiement_reussi -> finaliserDepuisLaNotification(evenement.intentionId());
            case paiement_rembourse -> enregistrerRemboursement(evenement.intentionId());
            // E1 : un paiement refusé ne crée rien, le membre peut réessayer.
            case paiement_echoue -> journal.info("Paiement {} refusé : aucun rendez-vous créé", evenement.intentionId());
        }
    }

    /** L'agent accepte la demande : demande -> confirme. Seul l'agent du rendez-vous peut le confirmer. */
    @Transactional
    public RendezVousResume confirmer(Integer id, Integer agentId, String ip) {
        RendezVous rdv = charger(id);
        exigerAgentDuRendezVous(rdv, agentId);
        rdv.confirmer(maintenant());
        audit.enregistrer(rdv.getAgent(), "confirmation_rdv", "rendez_vous#" + id, ip);
        publier(rdv, EvenementRendezVous.Type.confirme_par_agent);
        return RendezVousResume.pourAgent(rdv);
    }

    /**
     * Annulation par le membre qui a réservé ou par l'agent qui reçoit : demande/confirme -> annule.
     * RA8 : l'annulation d'un créneau payé rembourse le membre, sauf (RA14) s'il annule lui-même moins
     * de 24 heures avant la visite. Si le remboursement échoue, l'exception annule la transaction : le
     * rendez-vous reste confirmé, jamais « annulé mais non remboursé ».
     */
    @Transactional
    public RendezVousResume annuler(Integer id, Integer utilisateurId, String ip) {
        RendezVous rdv = charger(id);
        boolean parLAgent = rdv.getAgent().getId().equals(utilisateurId);
        if (!parLAgent && !rdv.getMembre().getId().equals(utilisateurId)) {
            throw new OperationInterditeException("Ce rendez-vous ne vous appartient pas");
        }
        annuler(rdv, parLAgent, ip);
        return parLAgent ? RendezVousResume.pourAgent(rdv) : RendezVousResume.pourMembre(rdv);
    }

    /**
     * Désinscription d'un membre (RA11) : ses visites à venir sont annulées comme s'il les annulait lui-même.
     * Le créneau redevient libre dans l'agenda de l'agent, qui est prévenu ; un créneau payé suit RA14.
     * Les visites passées restent telles quelles : elles font partie de l'historique conservé.
     *
     * @return le nombre de visites annulées
     */
    @Transactional
    public int annulerLesVisitesAVenir(Membre membre, String ip) {
        List<RendezVous> aVenir = rendezVous.findByMembreIdAndStatutInAndDateHeureAfter(membre.getId(), RendezVousRepository.ACTIFS, maintenant());
        aVenir.forEach(rdv -> annuler(rdv, false, ip));
        return aVenir.size();
    }

    private void annuler(RendezVous rdv, boolean parLAgent, String ip) {
        Integer id = rdv.getId();
        LocalDateTime maintenant = maintenant();
        boolean remboursable = rdv.remboursable(maintenant, parLAgent);
        rdv.annuler(maintenant);
        Utilisateur auteur = parLAgent ? rdv.getAgent() : rdv.getMembre();
        audit.enregistrer(auteur, "annulation_rdv", "rendez_vous#" + id, ip);

        Paiement paiement = rdv.getPaiement();
        if (paiement != null && paiement.getStatut() == StatutPaiement.reussi) {
            if (remboursable) {
                passerelle.rembourser(paiement.getStripePaymentIntentId());
                paiement.rembourser();
                audit.enregistrer(auteur, "remboursement", "paiement#" + paiement.getId(), ip);
            } else {
                audit.enregistrer(auteur, "annulation_tardive", "paiement#" + paiement.getId(), ip);
            }
        }
        publier(rdv, parLAgent ? EvenementRendezVous.Type.annule_par_agent : EvenementRendezVous.Type.annule_par_membre);
    }

    /** Demande dont la date est passée sans réponse : l'agent la classe sans suite (demande -> annule), sans e-mail. */
    @Transactional
    public RendezVousResume classerSansSuite(Integer id, Integer agentId, String ip) {
        RendezVous rdv = charger(id);
        exigerAgentDuRendezVous(rdv, agentId);
        rdv.classerSansSuite(maintenant());
        audit.enregistrer(rdv.getAgent(), "demande_sans_suite", "rendez_vous#" + id, ip);
        return RendezVousResume.pourAgent(rdv);
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

    // ---------- Créneau premium ----------

    private RendezVous reserverPremium(Membre membre, RequeteRendezVous requete, String ip) {
        Optional<Paiement> dejaEnregistre = paiements.findByStripePaymentIntentId(requete.paymentIntentId());
        if (dejaEnregistre.isPresent()) {
            return rendezVousDuPaiement(dejaEnregistre.get(), membre);
        }
        IntentionPaiement intention = intentionPayee(requete.paymentIntentId(), membre);
        if (!String.valueOf(requete.bienId()).equals(intention.metadonnees().get(META_BIEN))
                || !requete.dateHeure().equals(dateDe(intention))) {
            throw new DonneeInvalideException("paymentIntentId", "ce paiement ne correspond pas à ce créneau");
        }
        String motif = requete.motif() != null ? requete.motif() : intention.metadonnees().get(META_MOTIF);
        return creerRendezVousPaye(membre, requete.bienId(), requete.dateHeure(), motif, intention, ip);
    }

    /** Le paiement doit exister chez le prestataire, être réussi, et avoir été préparé par ce membre. */
    private IntentionPaiement intentionPayee(String id, Membre membre) {
        IntentionPaiement intention;
        try {
            intention = passerelle.consulter(id);
        } catch (IntentionIntrouvableException e) {
            throw new DonneeInvalideException("paymentIntentId", "paiement inconnu");
        }
        if (!String.valueOf(membre.getId()).equals(intention.metadonnees().get(META_MEMBRE))) {
            throw new DonneeInvalideException("paymentIntentId", "paiement inconnu");
        }
        if (!intention.estReussie()) {
            throw new DonneeInvalideException("paymentIntentId", "le paiement n'a pas abouti");
        }
        return intention;
    }

    /**
     * Étapes 8 du scénario nominal : rendez-vous confirmé et paiement réussi enregistrés ensemble.
     * Le membre a déjà payé : si la visite ne peut finalement pas être créée (créneau pris entre-temps,
     * bien retiré de la vente), il est remboursé avant que l'erreur ne lui soit renvoyée.
     */
    private RendezVous creerRendezVousPaye(Membre membre, Integer bienId, LocalDateTime dateHeure, String motif,
                                          IntentionPaiement intention, String origine) {
        try {
            Bien bien = bienVisitable(bienId);
            exigerReservable(dateHeure);
            if (!DEVISE.equals(intention.devise()) || intention.montantCentimes() != centimes(grille.prix(dateHeure))) {
                throw new DonneeInvalideException("paymentIntentId", "le montant payé ne correspond pas au prix du créneau");
            }
            verrouillerAgenda(bien);
            // Relu sous verrou : la réservation et la notification du prestataire peuvent arriver ensemble.
            Optional<Paiement> dejaEnregistre = paiements.findByStripePaymentIntentId(intention.id());
            if (dejaEnregistre.isPresent()) {
                return rendezVousDuPaiement(dejaEnregistre.get(), membre);
            }
            exigerCreneauLibre(membre, bien, dateHeure);

            RendezVous rdv = new RendezVous(membre, bien, dateHeure, nettoyer(motif));
            rdv.confirmer(maintenant());
            rendezVous.save(rdv);
            Paiement paiement = new Paiement(rdv, membre, intention.id(), grille.prix(dateHeure));
            paiement.marquerReussi();
            rdv.setPaiement(paiements.save(paiement));
            audit.enregistrer(membre, "paiement_reussi", "rendez_vous#" + rdv.getId(), origine);
            publier(rdv, EvenementRendezVous.Type.confirme_par_paiement);
            return rdv;
        } catch (CreneauIndisponibleException e) {
            rembourserFauteDeVisite(intention, e);
            throw new CreneauIndisponibleException(e.getMessage() + " Votre paiement est remboursé.");
        } catch (BienIndisponibleException | RessourceIntrouvableException | DonneeInvalideException e) {
            rembourserFauteDeVisite(intention, e);
            throw e;
        }
    }

    private void rembourserFauteDeVisite(IntentionPaiement intention, RuntimeException raison) {
        journal.warn("Paiement {} remboursé, visite impossible : {}", intention.id(), raison.getMessage());
        passerelle.rembourser(intention.id());
    }

    private void finaliserDepuisLaNotification(String intentionId) {
        if (paiements.findByStripePaymentIntentId(intentionId).isPresent()) {
            return;
        }
        IntentionPaiement intention = passerelle.consulter(intentionId);
        Map<String, String> meta = intention.metadonnees();
        if (!intention.estReussie() || !meta.containsKey(META_MEMBRE) || !meta.containsKey(META_BIEN) || !meta.containsKey(META_DATE)) {
            journal.info("Notification ignorée : le paiement {} ne concerne pas un créneau de visite", intentionId);
            return;
        }
        try {
            Membre membre = membre(Integer.valueOf(meta.get(META_MEMBRE)));
            creerRendezVousPaye(membre, Integer.valueOf(meta.get(META_BIEN)), dateDe(intention), meta.get(META_MOTIF),
                    intention, ORIGINE_WEBHOOK);
        } catch (IllegalStateException | RessourceIntrouvableException | DonneeInvalideException | OperationInterditeException e) {
            // Le remboursement est fait ; répondre 200 évite que le prestataire ne renvoie la notification en boucle.
            journal.warn("Paiement {} reçu mais visite non créée : {}", intentionId, e.getMessage());
        }
    }

    /** Remboursement décidé chez le prestataire (tableau de bord Stripe) : la visite payée est annulée. */
    private void enregistrerRemboursement(String intentionId) {
        paiements.findByStripePaymentIntentId(intentionId)
                .filter(paiement -> paiement.getStatut() == StatutPaiement.reussi)
                .ifPresent(paiement -> {
                    paiement.rembourser();
                    RendezVous rdv = paiement.getRendezVous();
                    if (RendezVousRepository.ACTIFS.contains(rdv.getStatut()) && rdv.getDateHeure().isAfter(maintenant())) {
                        rdv.annuler(maintenant());
                        publier(rdv, EvenementRendezVous.Type.annule_par_agent);
                    }
                    audit.enregistrer(paiement.getMembre(), "remboursement", "paiement#" + paiement.getId(), ORIGINE_WEBHOOK);
                });
    }

    /** Contrôle de propriété : la référence d'un paiement ne donne accès qu'au rendez-vous de son auteur. */
    private static RendezVous rendezVousDuPaiement(Paiement paiement, Membre membre) {
        if (!paiement.getMembre().getId().equals(membre.getId())) {
            throw new DonneeInvalideException("paymentIntentId", "paiement inconnu");
        }
        return paiement.getRendezVous();
    }

    private static LocalDateTime dateDe(IntentionPaiement intention) {
        try {
            return LocalDateTime.parse(intention.metadonnees().getOrDefault(META_DATE, ""));
        } catch (DateTimeParseException e) {
            throw new DonneeInvalideException("paymentIntentId", "ce paiement ne correspond pas à ce créneau");
        }
    }

    private static long centimes(BigDecimal montant) {
        return montant.movePointRight(2).longValueExact();
    }

    // ---------- Règles communes ----------

    /**
     * Verrouille l'agenda de l'agent le temps de la transaction : de deux réservations simultanées
     * du même créneau, la seconde attend, voit le rendez-vous de la première et est refusée (A2).
     */
    private void verrouillerAgenda(Bien bien) {
        Integer agentId = bien.getAgent().getId();
        agents.verrouiller(agentId).orElseThrow(() -> new RessourceIntrouvableException("Agent", agentId));
    }

    private void exigerCreneauLibre(Membre membre, Bien bien, LocalDateTime dateHeure) {
        if (rendezVous.existsByAgentIdAndDateHeureAndStatutIn(bien.getAgent().getId(), dateHeure, RendezVousRepository.ACTIFS)) {
            throw new CreneauIndisponibleException("Ce créneau vient d'être réservé par un autre membre.");
        }
        if (rendezVous.existsByMembreIdAndDateHeureAndStatutIn(membre.getId(), dateHeure, RendezVousRepository.ACTIFS)) {
            throw new CreneauIndisponibleException("Vous avez déjà une visite prévue à cette date.");
        }
    }

    private void exigerReservable(LocalDateTime dateHeure) {
        if (!grille.estReservable(dateHeure)) {
            throw new DonneeInvalideException("dateHeure", "ce créneau n'existe pas dans la grille des visites");
        }
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

    private Membre membre(Integer id) {
        return membres.findById(id).orElseThrow(() -> new OperationInterditeException("Seul un membre peut prendre rendez-vous"));
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

    /** Pattern Observer : le service annonce le changement d'état, les observateurs (e-mails) en font leur affaire. */
    private void publier(RendezVous rdv, EvenementRendezVous.Type type) {
        evenements.publishEvent(new EvenementRendezVous(rdv.getId(), type));
    }

    private static String nettoyer(String motif) {
        return motif == null || motif.isBlank() ? null : motif.trim();
    }

    private LocalDateTime maintenant() {
        return LocalDateTime.now(horloge);
    }
}
