package be.immoconnect.depot;

import be.immoconnect.entite.Article;
import be.immoconnect.entite.StatutArticle;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ArticleRepository extends JpaRepository<Article, Integer> {

    /** Articles d'un statut, éventuellement limités à une catégorie. */
    @EntityGraph(attributePaths = {"categorie", "administrateur"})
    @Query("select a from Article a where a.statut = :statut and (:categorieId is null or a.categorie.id = :categorieId)")
    Page<Article> parStatut(StatutArticle statut, Integer categorieId, Pageable pagination);

    @EntityGraph(attributePaths = {"categorie", "administrateur"})
    @Query("select a from Article a where (:statut is null or a.statut = :statut)")
    Page<Article> pourLeBackOffice(StatutArticle statut, Pageable pagination);

    @EntityGraph(attributePaths = {"categorie", "administrateur"})
    Optional<Article> findWithDetailsById(Integer id);

    long countByCategorieId(Integer categorieId);
}
