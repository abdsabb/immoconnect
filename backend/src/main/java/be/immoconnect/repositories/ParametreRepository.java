package be.immoconnect.repositories;

import be.immoconnect.entities.Parametre;
import org.springframework.data.jpa.repository.JpaRepository;

/** Paramètres du site (cas A5), une ligne par clé. */
public interface ParametreRepository extends JpaRepository<Parametre, String> {
}
