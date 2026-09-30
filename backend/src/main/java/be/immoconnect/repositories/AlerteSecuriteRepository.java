package be.immoconnect.repositories;

import be.immoconnect.entities.AlerteSecurite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Alertes de la détection d'intrusion, de la plus récente à la plus ancienne. */
public interface AlerteSecuriteRepository extends JpaRepository<AlerteSecurite, Integer> {

    Page<AlerteSecurite> findAllByOrderByCreeLeDescIdDesc(Pageable pagination);
}
