package be.immoconnect.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Rendez-vous de visite d'un bien — table rendez_vous.
 * Le cycle de vie (pattern State) applique le diagramme d'état-transition : seules les
 * transitions de ce diagramme sont acceptées (RA1), et confirmation/annulation ne sont
 * possibles qu'avant la date de la visite (RA2).
 */
@Entity
@Table(name = "rendez_vous")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RendezVous {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false)
    private AgentImmobilier agent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bien_id", nullable = false)
    private Bien bien;

    @Column(name = "date_heure", nullable = false)
    private LocalDateTime dateHeure;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('demande','confirme','annule','honore')")
    private StatutRendezVous statut = StatutRendezVous.demande;

    @Column(length = 255)
    private String motif;

    /** Lien 1-0..1 : un paiement au plus, uniquement pour un créneau premium (RA7). */
    @OneToOne(mappedBy = "rendezVous", fetch = FetchType.LAZY)
    private Paiement paiement;

    public RendezVous(Membre membre, Bien bien, LocalDateTime dateHeure, String motif) {
        this.membre = membre;
        this.agent = bien.getAgent();          // RA9 : l'agent du rendez-vous est l'agent du bien
        this.bien = bien;
        this.dateHeure = dateHeure;
        this.motif = motif;
    }

    /*
     * Les transitions reçoivent l'heure courante en paramètre : elle vient de l'horloge de
     * l'application (fuseau de l'agence), pas de celle du serveur.
     */

    /** Transition demande -> confirme (par l'agent, ou immédiate après paiement premium). */
    public void confirmer(LocalDateTime maintenant) {
        exigerTransition(statut == StatutRendezVous.demande, "confirmer");
        exigerDateFuture("confirmer", maintenant);
        this.statut = StatutRendezVous.confirme;
    }

    /** Transition demande/confirme -> annule ; un rendez-vous honoré ne s'annule jamais (RA1). */
    public void annuler(LocalDateTime maintenant) {
        exigerTransition(statut == StatutRendezVous.demande || statut == StatutRendezVous.confirme, "annuler");
        exigerDateFuture("annuler", maintenant);
        this.statut = StatutRendezVous.annule;
    }

    /** Transition confirme -> honore, uniquement une fois la date de la visite passée (RA2). */
    public void honorer(LocalDateTime maintenant) {
        exigerTransition(statut == StatutRendezVous.confirme, "honorer");
        if (dateHeure.isAfter(maintenant)) {
            throw new TransitionInterditeException("Impossible d'honorer un rendez-vous dont la date n'est pas passée (RA2)");
        }
        this.statut = StatutRendezVous.honore;
    }

    /**
     * Une demande restée sans réponse jusqu'à sa date ne peut plus être confirmée ni annulée (RA2) : l'agent la
     * classe sans suite, pour qu'elle ne reste pas indéfiniment « en attente ».
     */
    public void classerSansSuite(LocalDateTime maintenant) {
        exigerTransition(statut == StatutRendezVous.demande, "classer sans suite");
        if (dateHeure.isAfter(maintenant)) {
            throw new TransitionInterditeException("Cette demande est encore à venir : confirmez-la ou annulez-la (RA2)");
        }
        this.statut = StatutRendezVous.annule;
    }

    public boolean estPremium() {
        return paiement != null;
    }

    /** Délai de rétractation gratuite d'un créneau premium (règle RA14, conditions générales). */
    public static final Duration PREAVIS_REMBOURSEMENT = Duration.ofHours(24);

    /** Dernier instant où le membre peut encore annuler un créneau premium en étant remboursé ; null si rien n'a été payé. */
    public LocalDateTime remboursableJusquA() {
        return estPremium() ? dateHeure.minus(PREAVIS_REMBOURSEMENT) : null;
    }

    /**
     * RA14 : le membre qui annule un créneau premium moins de 24 heures avant la visite n'est pas
     * remboursé ; l'agent qui annule rembourse toujours (RA8).
     */
    public boolean remboursable(LocalDateTime maintenant, boolean parLAgent) {
        return estPremium() && (parLAgent || !maintenant.isAfter(remboursableJusquA()));
    }

    private void exigerTransition(boolean autorisee, String operation) {
        if (!autorisee) {
            throw new TransitionInterditeException(
                    "Impossible de " + operation + " un rendez-vous au statut « " + statut + " » (RA1)");
        }
    }

    private void exigerDateFuture(String operation, LocalDateTime maintenant) {
        if (!dateHeure.isAfter(maintenant)) {
            throw new TransitionInterditeException("Impossible de " + operation + " un rendez-vous dont la date est passée (RA2)");
        }
    }

    /** Levée quand une transition interdite par le diagramme d'état est tentée. */
    public static class TransitionInterditeException extends IllegalStateException {
        public TransitionInterditeException(String message) {
            super(message);
        }
    }
}
