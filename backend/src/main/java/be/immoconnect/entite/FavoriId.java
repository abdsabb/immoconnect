package be.immoconnect.entite;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Clé primaire composée de la table de jonction favori (membre_id, bien_id). */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class FavoriId implements Serializable {

    @Column(name = "membre_id", nullable = false)
    private Integer membreId;

    @Column(name = "bien_id", nullable = false)
    private Integer bienId;
}
