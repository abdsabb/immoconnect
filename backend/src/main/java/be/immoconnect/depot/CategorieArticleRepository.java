package be.immoconnect.depot;

import be.immoconnect.entite.CategorieArticle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategorieArticleRepository extends JpaRepository<CategorieArticle, Integer> {
}
