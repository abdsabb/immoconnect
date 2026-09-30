package be.immoconnect.repositories;

import be.immoconnect.entities.Message;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MessageRepository extends JpaRepository<Message, Integer> {

    List<Message> findByMembreId(Integer membreId);

    /** Tous les messages d'un membre, du plus récent au plus ancien : de quoi bâtir la liste de ses conversations. */
    @EntityGraph(attributePaths = {"membre", "agent"})
    List<Message> findByMembreIdOrderByEnvoyeLeDescIdDesc(Integer membreId);

    @EntityGraph(attributePaths = {"membre", "agent"})
    List<Message> findByAgentIdOrderByEnvoyeLeDescIdDesc(Integer agentId);

    /** Une conversation, dans l'ordre de lecture. */
    @EntityGraph(attributePaths = {"membre", "agent"})
    List<Message> findByMembreIdAndAgentIdOrderByEnvoyeLeAscIdAsc(Integer membreId, Integer agentId);

    boolean existsByMembreIdAndAgentId(Integer membreId, Integer agentId);

    /** Marque comme lus les messages d'une conversation écrits par l'autre partie. */
    @Modifying(clearAutomatically = true)
    @Query("""
            update Message m set m.lu = true
            where m.membre.id = :membreId and m.agent.id = :agentId and m.expediteur = :auteur and m.lu = false
            """)
    int marquerLus(Integer membreId, Integer agentId, Message.Expediteur auteur);
}
