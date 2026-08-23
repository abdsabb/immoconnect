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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Texte d'interface traduit, identifié par (langue, clé) — table traduction. */
@Entity
@Table(name = "traduction")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Traduction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "langue_id", nullable = false)
    private Langue langue;

    @Column(nullable = false, length = 120)
    private String cle;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String valeur;
}
