package be.immoconnect.repositories;

import be.immoconnect.entities.AgentImmobilier;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface AgentImmobilierRepository extends JpaRepository<AgentImmobilier, Integer> {

    /**
     * Verrouille la ligne de l'agent (SELECT … FOR UPDATE) jusqu'à la fin de la transaction.
     * Deux réservations simultanées dans le même agenda passent ainsi l'une après l'autre : la
     * seconde voit le rendez-vous créé par la première et reçoit un conflit 409 (scénario A2).
     */
    long countByMatriculeStartingWith(String prefixe);

    boolean existsByMatricule(String matricule);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AgentImmobilier a where a.id = :id")
    Optional<AgentImmobilier> verrouiller(Integer id);
}
