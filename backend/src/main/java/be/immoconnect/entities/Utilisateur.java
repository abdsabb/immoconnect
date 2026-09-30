package be.immoconnect.entities;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Compte utilisateur (classe abstraite du diagramme de classes).
 * L'héritage Utilisateur -> Membre / AgentImmobilier / Administrateur est réalisé en
 * tables jointes (JOINED) : une table mère et trois tables filles partageant la clé,
 * fidèlement au schéma du livrable 08. La colonne ENUM role sert de discriminant.
 */
@Entity
@Table(name = "utilisateur")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "role", discriminatorType = DiscriminatorType.STRING,
        columnDefinition = "ENUM('membre','agent','admin')")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 80)
    private String nom;

    @Column(nullable = false, length = 80)
    private String prenom;

    @Column(nullable = false, unique = true, length = 190)
    private String email;

    /** Jamais en clair : haché BCrypt (livrable 16). */
    @Column(name = "mot_de_passe", nullable = false)
    private String motDePasse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "langue_id", nullable = false)
    private Langue langue;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "date_inscription", nullable = false)
    private LocalDate dateInscription;

    /** Un compte désactivé par l'administrateur ne peut plus se connecter (cas A1) ; rien n'est supprimé. */
    @Column(nullable = false)
    private boolean actif = true;

    /** Adresse confirmée par le lien d'activation ; tant qu'elle ne l'est pas, le compte ne se connecte pas. */
    @Column(name = "email_verifie", nullable = false)
    private boolean emailVerifie = false;

    /** Choix d'un membre ; pour un agent ou un administrateur, le second facteur est imposé par le rôle. */
    @Column(name = "double_facteur", nullable = false)
    private boolean doubleFacteur = false;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "echecs_connexion", nullable = false)
    private Integer echecsConnexion = 0;

    @Column(name = "verrouille_jusqu_a")
    private LocalDateTime verrouilleJusquA;

    @Column(name = "cgu_acceptees_le")
    private LocalDateTime cguAccepteesLe;

    /** Consentement aux communications non essentielles : refusé par défaut, retirable à tout moment. */
    @Column(name = "consentement_communications", nullable = false)
    private boolean consentementCommunications = false;

    protected Utilisateur(String nom, String prenom, String email, String motDePasseHache, Langue langue) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.motDePasse = motDePasseHache;
        this.langue = langue;
        this.dateInscription = LocalDate.now();
    }

    /** Rôle applicatif, tel que stocké dans la colonne discriminante (membre, agent, admin). */
    public abstract String getRole();

    /** Un compte qui donne accès à des données de tiers exige toujours un second facteur. */
    public boolean exigeDoubleFacteur() {
        return doubleFacteur || !"membre".equals(getRole());
    }

    public boolean estVerrouille(LocalDateTime maintenant) {
        return verrouilleJusquA != null && verrouilleJusquA.isAfter(maintenant);
    }

    public String getNomComplet() {
        return prenom + " " + nom;
    }
}
