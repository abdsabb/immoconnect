package be.immoconnect.depot;

import be.immoconnect.entite.Membre;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembreRepository extends JpaRepository<Membre, Integer> {
}
