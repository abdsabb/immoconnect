package be.immoconnect.depot;

import be.immoconnect.entite.CleApi;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CleApiRepository extends JpaRepository<CleApi, Integer> {

    Optional<CleApi> findByEmpreinte(String empreinte);

    @EntityGraph(attributePaths = {"administrateur"})
    List<CleApi> findAllByOrderByCreeLeDescIdDesc();
}
