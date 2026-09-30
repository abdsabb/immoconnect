package be.immoconnect.repositories;

import be.immoconnect.entities.RendezVous;
import be.immoconnect.entities.StatutRendezVous;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RendezVousRepository extends JpaRepository<RendezVous, Integer> {

    /** Statuts qui occupent un créneau : un rendez-vous annulé ou honoré libère l'agenda. */
    List<StatutRendezVous> ACTIFS = List.of(StatutRendezVous.demande, StatutRendezVous.confirme);

    @EntityGraph(attributePaths = {"membre", "agent", "bien", "paiement"})
    List<RendezVous> findByMembreIdOrderByDateHeureDesc(Integer membreId);

    @EntityGraph(attributePaths = {"membre", "agent", "bien", "paiement"})
    List<RendezVous> findByAgentIdOrderByDateHeureDesc(Integer agentId);

    @EntityGraph(attributePaths = {"membre", "agent", "bien", "paiement"})
    Optional<RendezVous> findWithDetailsById(Integer id);

    boolean existsByAgentIdAndDateHeureAndStatutIn(Integer agentId, LocalDateTime dateHeure, Collection<StatutRendezVous> statuts);

    boolean existsByMembreIdAndDateHeureAndStatutIn(Integer membreId, LocalDateTime dateHeure, Collection<StatutRendezVous> statuts);

    boolean existsByBienIdAndStatutInAndDateHeureAfter(Integer bienId, Collection<StatutRendezVous> statuts, LocalDateTime apres);

    /** Visites à venir par bien et par statut, pour un agent : lignes [identifiant du bien, statut, nombre]. */
    @Query("""
            select r.bien.id, r.statut, count(r) from RendezVous r
            where r.agent.id = :agentId and r.statut in :statuts and r.dateHeure > :apres
            group by r.bien.id, r.statut
            """)
    List<Object[]> compterAVenirParBien(Integer agentId, Collection<StatutRendezVous> statuts, LocalDateTime apres);

    /** Créneaux déjà pris dans l'agenda d'un agent sur une période. */
    @Query("""
            select r.dateHeure from RendezVous r
            where r.agent.id = :agentId and r.statut in :statuts and r.dateHeure between :du and :au
            """)
    List<LocalDateTime> creneauxOccupes(Integer agentId, Collection<StatutRendezVous> statuts, LocalDateTime du, LocalDateTime au);
}
