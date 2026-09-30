package be.immoconnect.repositories;

import be.immoconnect.entities.Langue;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LangueRepository extends JpaRepository<Langue, Integer> {

    Optional<Langue> findByCode(String code);
}
