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

/** Langues de l'interface (FR/NL/EN) — table langue. */
@Entity
@Table(name = "langue")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Langue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Code ISO 639-1 en minuscules ('fr', 'nl', 'en'). */
    @Column(nullable = false, unique = true, columnDefinition = "CHAR(2)")
    private String code;

    @Column(nullable = false, length = 40)
    private String nom;
}
