package be.immoconnect.entite;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Clé d'accès à l'API pour les consommateurs externes (Open Data, partenaires) — table cle_api. */
@Entity
@Table(name = "cle_api")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CleApi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "administrateur_id", nullable = false)
    private Administrateur administrateur;

    /** 64 caractères hexadécimaux générés aléatoirement, uniques. */
    @Column(nullable = false, unique = true, length = 64)
    private String cle;

    @Column(name = "cree_le", nullable = false)
    private LocalDate creeLe;

    @Column(nullable = false)
    private boolean active;

    public CleApi(Administrateur administrateur, String cle) {
        this.administrateur = administrateur;
        this.cle = cle;
        this.creeLe = LocalDate.now();
        this.active = true;
    }

    /** RA12 : une clé révoquée refuse immédiatement tout appel. */
    public void revoquer() {
        this.active = false;
    }
}
