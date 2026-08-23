package be.immoconnect.entite;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Annonce immobilière publiée par un agent — table bien (classe centrale de l'analyse). */
@Entity
@Table(name = "bien")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bien {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false)
    private AgentImmobilier agent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categorie_id", nullable = false)
    private Categorie categorie;

    @Column(nullable = false, length = 150)
    private String titre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /** Prix demandé en euros, strictement positif (règle du domaine). */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal prix;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal superficie;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "nb_chambres", nullable = false)
    private Integer nbChambres;

    @Column(nullable = false, length = 150)
    private String adresse;

    @Column(nullable = false, length = 80)
    private String ville;

    @Column(name = "code_postal", nullable = false, length = 10)
    private String codePostal;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('disponible','sous_option','vendu','loue','archive')")
    private StatutBien statut = StatutBien.disponible;

    @Column(name = "publie_le", nullable = false)
    private LocalDate publieLe;

    /** Composition 1..* : les photos n'existent pas sans leur bien (ON DELETE CASCADE, règle RA10). */
    @OneToMany(mappedBy = "bien", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordre ASC")
    private List<Photo> photos = new ArrayList<>();

    /** Un bien n'accepte ni rendez-vous ni favori s'il n'est plus disponible (RA5). */
    public boolean estDisponible() {
        return statut.estDisponible();
    }
}
