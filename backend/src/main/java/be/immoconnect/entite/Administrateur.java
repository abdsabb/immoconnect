package be.immoconnect.entite;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Administrateur de la plateforme (niveau 1 éditeur, 2 gestionnaire, 3 super-admin) — table fille administrateur. */
@Entity
@Table(name = "administrateur")
@PrimaryKeyJoinColumn(name = "utilisateur_id")
@DiscriminatorValue("admin")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Administrateur extends Utilisateur {

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "niveau_acces", nullable = false)
    private Integer niveauAcces;

    @Override
    public String getRole() {
        return "admin";
    }
}
