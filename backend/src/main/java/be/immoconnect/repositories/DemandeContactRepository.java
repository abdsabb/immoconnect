package be.immoconnect.repositories;

import be.immoconnect.entities.DemandeContact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Demandes de contact, de la plus récente à la plus ancienne. */
public interface DemandeContactRepository extends JpaRepository<DemandeContact, Integer> {

    @EntityGraph(attributePaths = {"traitePar"})
    Page<DemandeContact> findAllByOrderByCreeLeDescIdDesc(Pageable pagination);

    @EntityGraph(attributePaths = {"traitePar"})
    Page<DemandeContact> findByTraiteLeIsNullOrderByCreeLeDescIdDesc(Pageable pagination);
}
