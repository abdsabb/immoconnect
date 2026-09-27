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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Clé d'accès à l'API pour les consommateurs externes (Open Data, partenaires) — table cle_api.
 * La base ne conserve que l'empreinte SHA-256 de la clé : la clé elle-même n'est montrée qu'une
 * fois, à sa création, et ne peut plus être retrouvée ensuite.
 */
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

    /** Empreinte SHA-256 de la clé, en 64 caractères hexadécimaux, unique. */
    @Column(name = "cle", nullable = false, unique = true, length = 64)
    private String empreinte;

    /** À qui la clé a été délivrée. */
    @Column(length = 80)
    private String libelle;

    @Column(name = "cree_le", nullable = false)
    private LocalDate creeLe;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "derniere_utilisation")
    private LocalDateTime derniereUtilisation;

    public CleApi(Administrateur administrateur, String empreinte, String libelle, LocalDate creeLe) {
        this.administrateur = administrateur;
        this.empreinte = empreinte;
        this.libelle = libelle;
        this.creeLe = creeLe;
        this.active = true;
    }

    /** RA12 : une clé révoquée refuse immédiatement tout appel. */
    public void revoquer() {
        this.active = false;
    }

    public void noterUtilisation(LocalDateTime moment) {
        this.derniereUtilisation = moment;
    }
}
