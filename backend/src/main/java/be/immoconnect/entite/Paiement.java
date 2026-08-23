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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Paiement Stripe d'un créneau premium (0 ou 1 par rendez-vous) — table paiement. */
@Entity
@Table(name = "paiement")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Paiement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rendez_vous_id", nullable = false, unique = true)
    private RendezVous rendezVous;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    /** Référence PaymentIntent Stripe (format pi_…), unique. */
    @Column(name = "stripe_payment_intent_id", nullable = false, unique = true, length = 120)
    private String stripePaymentIntentId;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('initie','reussi','echoue','rembourse')")
    private StatutPaiement statut = StatutPaiement.initie;

    @Column(name = "paye_le")
    private LocalDateTime payeLe;

    public Paiement(RendezVous rendezVous, Membre membre, String stripePaymentIntentId, BigDecimal montant) {
        this.rendezVous = rendezVous;
        this.membre = membre;
        this.stripePaymentIntentId = stripePaymentIntentId;
        this.montant = montant;
    }

    /** Confirmation par le webhook Stripe : initie -> reussi, date de paiement renseignée. */
    public void marquerReussi() {
        exiger(statut == StatutPaiement.initie, "confirmer");
        this.statut = StatutPaiement.reussi;
        this.payeLe = LocalDateTime.now();
    }

    public void marquerEchoue() {
        exiger(statut == StatutPaiement.initie, "refuser");
        this.statut = StatutPaiement.echoue;
    }

    /** RA3 : un paiement réussi ne peut plus être modifié, seule la transition vers remboursé est autorisée (RA8). */
    public void rembourser() {
        exiger(statut == StatutPaiement.reussi, "rembourser");
        this.statut = StatutPaiement.rembourse;
    }

    private void exiger(boolean autorise, String operation) {
        if (!autorise) {
            throw new IllegalStateException("Impossible de " + operation + " un paiement au statut « " + statut + " » (RA3)");
        }
    }
}
