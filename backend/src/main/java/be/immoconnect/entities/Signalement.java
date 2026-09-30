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
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Signalement d'un contenu par un utilisateur — table signalement. Le règlement européen sur les
 * services numériques impose à l'intermédiaire d'agir promptement dès qu'un contenu illicite lui est
 * signalé (chapitre 11 du rapport) : l'administrateur tranche, retire ou conserve, et motive sa décision.
 */
@Entity
@Table(name = "signalement")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Signalement {

    public enum TypeContenu { message, bien, article }

    public enum Motif { illicite, arnaque, indesirable, autre }

    public enum Statut { ouvert, retire, conserve }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auteur_id", nullable = false)
    private Utilisateur auteur;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_contenu", nullable = false, columnDefinition = "ENUM('message','bien','article')")
    private TypeContenu typeContenu;

    @Column(name = "contenu_id", nullable = false)
    private Integer contenuId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('illicite','arnaque','indesirable','autre')")
    private Motif motif;

    @Column(nullable = false, length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('ouvert','retire','conserve')")
    private Statut statut = Statut.ouvert;

    @Column(name = "cree_le", nullable = false)
    private LocalDateTime creeLe;

    @Column(name = "traite_le")
    private LocalDateTime traiteLe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "traite_par")
    private Administrateur traitePar;

    @Column(length = 500)
    private String decision;

    public Signalement(Utilisateur auteur, TypeContenu typeContenu, Integer contenuId, Motif motif, String description,
                       LocalDateTime maintenant) {
        this.auteur = auteur;
        this.typeContenu = typeContenu;
        this.contenuId = contenuId;
        this.motif = motif;
        this.description = description;
        this.creeLe = maintenant;
    }

    /** La décision est définitive : un signalement traité ne se rouvre pas. */
    public void trancher(Statut decision, String motivation, Administrateur administrateur, LocalDateTime maintenant) {
        if (statut != Statut.ouvert) {
            throw new IllegalStateException("Ce signalement a déjà été traité");
        }
        if (decision == Statut.ouvert) {
            throw new IllegalArgumentException("La décision est « retire » ou « conserve »");
        }
        this.statut = decision;
        this.decision = motivation;
        this.traitePar = administrateur;
        this.traiteLe = maintenant;
    }
}
