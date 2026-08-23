package be.immoconnect.entite;

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

    /** Transition demande -> confirme (par l'agent, ou immédiate après paiement premium). */
    public void confirmer() {
        exigerTransition(statut == StatutRendezVous.demande, "confirmer");
        exigerDateFuture("confirmer");
        this.statut = StatutRendezVous.confirme;
    }

    /** Transition demande/confirme -> annule ; un rendez-vous honoré ne s'annule jamais (RA1). */
    public void annuler() {
        exigerTransition(statut == StatutRendezVous.demande || statut == StatutRendezVous.confirme, "annuler");
        exigerDateFuture("annuler");
        this.statut = StatutRendezVous.annule;
    }

    /** Transition confirme -> honore, uniquement une fois la date de la visite passée (RA2). */
    public void honorer() {
        exigerTransition(statut == StatutRendezVous.confirme, "honorer");
        if (dateHeure.isAfter(LocalDateTime.now())) {
            throw new TransitionInterditeException("Impossible d'honorer un rendez-vous dont la date n'est pas passée");
        }
        this.statut = StatutRendezVous.honore;
    }

    public boolean estPremium() {
        return paiement != null;
    }

    private void exigerTransition(boolean autorisee, String operation) {
        if (!autorisee) {
            throw new TransitionInterditeException(
                    "Impossible de " + operation + " un rendez-vous au statut « " + statut + " » (RA1)");
        }
    }

    private void exigerDateFuture(String operation) {
        if (!dateHeure.isAfter(LocalDateTime.now())) {
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
