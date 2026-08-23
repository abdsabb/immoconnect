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

/**
 * Entrée du journal d'audit (règle RA13) : qui, quoi, quand, depuis quelle adresse IP.
 * Immuable après création — une ligne d'audit ne se modifie jamais.
 */
@Entity
@Table(name = "journal_audit")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JournalAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Column(nullable = false, length = 60)
    private String action;

    @Column(nullable = false, length = 60)
    private String entite;

    @Column(nullable = false)
    private LocalDateTime horodatage;

    @Column(nullable = false, length = 45)
    private String ip;

    public JournalAudit(Utilisateur utilisateur, String action, String entite, String ip) {
        this.utilisateur = utilisateur;
        this.action = action;
        this.entite = entite;
        this.ip = ip;
        this.horodatage = LocalDateTime.now();
    }
}
