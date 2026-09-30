package be.immoconnect.entities;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Agent immobilier : publie les biens, gère les visites — table fille agent_immobilier. */
@Entity
@Table(name = "agent_immobilier")
@PrimaryKeyJoinColumn(name = "utilisateur_id")
@DiscriminatorValue("agent")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgentImmobilier extends Utilisateur {

    @Column(nullable = false, unique = true, length = 20)
    private String matricule;

    @Column(name = "telephone_pro", nullable = false, length = 20)
    private String telephonePro;

    public AgentImmobilier(String nom, String prenom, String email, String motDePasseHache, Langue langue,
                           String matricule, String telephonePro) {
        super(nom, prenom, email, motDePasseHache, langue);
        this.matricule = matricule;
        this.telephonePro = telephonePro;
    }

    @Override
    public String getRole() {
        return "agent";
    }
}
