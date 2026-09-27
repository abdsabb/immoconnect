package be.immoconnect.depot;

import be.immoconnect.entite.Categorie;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategorieRepository extends JpaRepository<Categorie, Integer> {

    Optional<Categorie> findByNomIgnoreCase(String nom);
}
