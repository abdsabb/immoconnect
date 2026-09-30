package be.immoconnect.repositories;

import be.immoconnect.entities.Traduction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TraductionRepository extends JpaRepository<Traduction, Integer> {

    List<Traduction> findByLangueCode(String code);

    @EntityGraph(attributePaths = {"langue"})
    List<Traduction> findAllByOrderByCleAsc();

    Optional<Traduction> findByLangueCodeAndCle(String code, String cle);

    List<Traduction> findByCle(String cle);
}
