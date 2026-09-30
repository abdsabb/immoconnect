package be.immoconnect.repositories;

import be.immoconnect.entities.Bien;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Dépôt des biens (pattern Repository) ; la recherche multicritères passe par des Specifications. */
public interface BienRepository extends JpaRepository<Bien, Integer>, JpaSpecificationExecutor<Bien> {

    /** Détail d'un bien avec ses photos, son agent et sa catégorie chargés en une requête. */
    @EntityGraph(attributePaths = {"photos", "agent", "categorie"})
    Optional<Bien> findWithDetailsById(Integer id);

    /** Toutes les annonces d'un agent, hors ligne comprises, pour son tableau de bord (cas AG6). */
    @EntityGraph(attributePaths = {"categorie"})
    List<Bien> findByAgentIdOrderByPublieLeDescIdDesc(Integer agentId);

    long countByCategorieId(Integer categorieId);

    /** Une vue de plus sur la fiche publique (cas AG6), comptée en base : deux lecteurs simultanés ne se perdent pas. */
    @Modifying
    @Query("update Bien b set b.nbVues = b.nbVues + 1 where b.id = :id")
    void compterUneVue(Integer id);
}
