package be.immoconnect.entite;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/**
 * Message de la messagerie interne — table message. Une conversation réunit un membre et un
 * agent ; chaque message dit lequel des deux l'a écrit, l'autre en est le destinataire.
 */
@Entity
@Table(name = "message")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Message {

    public enum Expediteur {
        membre, agent
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false)
    private AgentImmobilier agent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('membre','agent')")
    private Expediteur expediteur;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenu;

    @Column(name = "envoye_le", nullable = false)
    private LocalDateTime envoyeLe;

    /** Lu par son destinataire. */
    @Column(nullable = false)
    private boolean lu;

    public Message(Membre membre, AgentImmobilier agent, Expediteur expediteur, String contenu, LocalDateTime envoyeLe) {
        this.membre = membre;
        this.agent = agent;
        this.expediteur = expediteur;
        this.contenu = contenu;
        this.envoyeLe = envoyeLe;
        this.lu = false;
    }

    public Utilisateur getAuteur() {
        return expediteur == Expediteur.membre ? membre : agent;
    }

    public Utilisateur getDestinataire() {
        return expediteur == Expediteur.membre ? agent : membre;
    }

    public void marquerLu() {
        this.lu = true;
    }
}
