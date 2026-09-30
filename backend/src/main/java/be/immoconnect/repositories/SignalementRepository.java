package be.immoconnect.repositories;

import be.immoconnect.entities.Signalement;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Signalements de contenus, lus par l'administrateur du plus récent au plus ancien. */
public interface SignalementRepository extends JpaRepository<Signalement, Integer> {

    @EntityGraph(attributePaths = {"auteur", "traitePar"})
    Page<Signalement> findByStatutOrderByCreeLeDescIdDesc(Signalement.Statut statut, Pageable pagination);

    @EntityGraph(attributePaths = {"auteur", "traitePar"})
    Page<Signalement> findAllByOrderByCreeLeDescIdDesc(Pageable pagination);

    long countByStatut(Signalement.Statut statut);

    List<Signalement> findByAuteurIdOrderByCreeLeDesc(Integer auteurId);

    boolean existsByAuteurIdAndTypeContenuAndContenuIdAndStatut(Integer auteurId, Signalement.TypeContenu type,
                                                              Integer contenuId, Signalement.Statut statut);
}
