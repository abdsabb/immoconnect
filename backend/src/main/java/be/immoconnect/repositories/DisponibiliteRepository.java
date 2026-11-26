package be.immoconnect.repositories;

import be.immoconnect.entities.Disponibilite;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DisponibiliteRepository extends JpaRepository<Disponibilite, Integer> {

    /** Plages d'un agent qui touchent la période [du, au[, dans l'ordre chronologique. */
    @Query("""
            select d from Disponibilite d
            where d.agent.id = :agentId and d.debut < :au and d.fin > :du
            order by d.debut
            """)
    List<Disponibilite> dansLaPeriode(Integer agentId, LocalDateTime du, LocalDateTime au);

    /** La plage de l'agent qui couvre l'instant donné, s'il en a ouvert une. */
    @Query("select d from Disponibilite d where d.agent.id = :agentId and d.debut <= :instant and d.fin > :instant")
    Optional<Disponibilite> couvrant(Integer agentId, LocalDateTime instant);

    /** Début de la dernière plage ouverte par l'agent ; vide s'il n'en a jamais ouvert. */
    @Query("select max(d.debut) from Disponibilite d where d.agent.id = :agentId")
    Optional<LocalDateTime> dernierDebut(Integer agentId);
}
