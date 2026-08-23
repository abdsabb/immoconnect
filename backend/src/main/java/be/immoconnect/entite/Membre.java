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

/** Membre / client : favoris, messages, rendez-vous, paiements — table fille membre. */
@Entity
@Table(name = "membre")
@PrimaryKeyJoinColumn(name = "utilisateur_id")
@DiscriminatorValue("membre")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Membre extends Utilisateur {

    @Column(length = 20)
    private String telephone;

    public Membre(String nom, String prenom, String email, String motDePasseHache, Langue langue, String telephone) {
        super(nom, prenom, email, motDePasseHache, langue);
        this.telephone = telephone;
    }

    @Override
    public String getRole() {
        return "membre";
    }
}
