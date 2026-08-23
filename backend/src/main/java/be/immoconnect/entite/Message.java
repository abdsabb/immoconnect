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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Message d'un membre à un agent immobilier (messagerie interne) — table message. */
@Entity
@Table(name = "message")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false)
    private AgentImmobilier agent;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenu;

    @Column(name = "envoye_le", nullable = false)
    private LocalDateTime envoyeLe;

    @Column(nullable = false)
    private boolean lu;

    public Message(Membre membre, AgentImmobilier agent, String contenu) {
        this.membre = membre;
        this.agent = agent;
        this.contenu = contenu;
        this.envoyeLe = LocalDateTime.now();
        this.lu = false;
    }

    public void marquerLu() {
        this.lu = true;
    }
}
