package be.immoconnect.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Message laissé par le formulaire de contact — table demande_contact. Son auteur n'a pas forcément de compte :
 * la demande porte ses coordonnées. Un administrateur la marque « traitée » une fois la réponse donnée.
 */
@Entity
@Table(name = "demande_contact")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DemandeContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(length = 30)
    private String telephone;

    @Column(nullable = false, length = 150)
    private String sujet;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "cree_le", nullable = false)
    private LocalDateTime creeLe;

    @Column(nullable = false, length = 45)
    private String ip;

    @Column(name = "traite_le")
    private LocalDateTime traiteLe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "traite_par")
    private Administrateur traitePar;

    public DemandeContact(String nom, String email, String telephone, String sujet, String message, LocalDateTime creeLe, String ip) {
        this.nom = nom;
        this.email = email;
        this.telephone = telephone;
        this.sujet = sujet;
        this.message = message;
        this.creeLe = creeLe;
        this.ip = ip;
    }

    public boolean estTraitee() {
        return traiteLe != null;
    }

    public void traiter(Administrateur administrateur, LocalDateTime maintenant) {
        this.traitePar = administrateur;
        this.traiteLe = maintenant;
    }
}
