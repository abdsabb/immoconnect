package be.immoconnect.repositories;

import be.immoconnect.entities.Administrateur;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdministrateurRepository extends JpaRepository<Administrateur, Integer> {
}
