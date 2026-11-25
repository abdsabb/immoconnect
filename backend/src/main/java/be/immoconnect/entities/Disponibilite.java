package be.immoconnect.entities;

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
 * Plage pendant laquelle un agent reçoit en visite — table disponibilite. L'agent l'ouvre lui-même dans son
 * agenda ; les créneaux proposés aux membres sont découpés dans ces plages, et nulle part ailleurs (RA15).
 * Une plage tient dans une seule journée.
 */
@Entity
@Table(name = "disponibilite")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Disponibilite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false)
    private AgentImmobilier agent;

    @Column(nullable = false)
    private LocalDateTime debut;

    @Column(nullable = false)
    private LocalDateTime fin;

    @Column(name = "cree_le", nullable = false)
    private LocalDateTime creeLe;

    public Disponibilite(AgentImmobilier agent, LocalDateTime debut, LocalDateTime fin, LocalDateTime creeLe) {
        this.agent = agent;
        this.debut = debut;
        this.fin = fin;
        this.creeLe = creeLe;
    }

    /** La plage couvre l'instant donné : début inclus, fin exclue. */
    public boolean couvre(LocalDateTime instant) {
        return !instant.isBefore(debut) && instant.isBefore(fin);
    }
}
