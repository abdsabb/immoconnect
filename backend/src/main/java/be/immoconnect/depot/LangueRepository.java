package be.immoconnect.depot;

import be.immoconnect.entite.Langue;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LangueRepository extends JpaRepository<Langue, Integer> {

    Optional<Langue> findByCode(String code);
}
