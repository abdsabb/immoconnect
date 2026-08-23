package be.immoconnect.depot;

import be.immoconnect.entite.Traduction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TraductionRepository extends JpaRepository<Traduction, Integer> {

    List<Traduction> findByLangueCode(String code);
}
