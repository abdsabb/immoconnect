package be.immoconnect.entite;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Catégorie de bien (maison, appartement, studio, terrain, commerce…) — table categorie. */
@Entity
@Table(name = "categorie")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Categorie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 60)
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String description;
}
