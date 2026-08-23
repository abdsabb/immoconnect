package be.immoconnect.depot;

import be.immoconnect.entite.Bien;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Dépôt des biens (pattern Repository) ; la recherche multicritères passe par des Specifications. */
public interface BienRepository extends JpaRepository<Bien, Integer>, JpaSpecificationExecutor<Bien> {

    /** Détail d'un bien avec ses photos, son agent et sa catégorie chargés en une requête. */
    @EntityGraph(attributePaths = {"photos", "agent", "categorie"})
    Optional<Bien> findWithDetailsById(Integer id);
}
