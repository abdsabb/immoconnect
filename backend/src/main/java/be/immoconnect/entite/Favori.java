package be.immoconnect.entite;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Classe-association Favori de l'analyse (Membre <-> Bien), matérialisée par la table de jonction
 * favori qui porte la date d'ajout.
 */
@Entity
@Table(name = "favori")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Favori {

    @EmbeddedId
    private FavoriId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("membreId")
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("bienId")
    @JoinColumn(name = "bien_id", nullable = false)
    private Bien bien;

    @Column(name = "date_ajout", nullable = false)
    private LocalDateTime dateAjout;

    public Favori(Membre membre, Bien bien) {
        this.membre = membre;
        this.bien = bien;
        this.id = new FavoriId(membre.getId(), bien.getId());
        this.dateAjout = LocalDateTime.now();
    }
}
