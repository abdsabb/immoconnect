package be.immoconnect.repositories;

import be.immoconnect.entities.Categorie;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategorieRepository extends JpaRepository<Categorie, Integer> {

    Optional<Categorie> findByNomIgnoreCase(String nom);
}
