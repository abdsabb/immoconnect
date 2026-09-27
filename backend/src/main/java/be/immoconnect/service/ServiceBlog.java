package be.immoconnect.service;

import be.immoconnect.api.blog.ArticleVue;
import be.immoconnect.api.blog.RequeteArticle;
import be.immoconnect.depot.ArticleRepository;
import be.immoconnect.depot.CategorieArticleRepository;
import be.immoconnect.entite.Administrateur;
import be.immoconnect.entite.Article;
import be.immoconnect.entite.CategorieArticle;
import be.immoconnect.entite.StatutArticle;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Blog : « Lire les articles du blog » (V5) côté public, « Gérer les articles du blog » (A2) côté
 * back-office. RA4 : le public ne voit que les articles publiés — un brouillon ou un article archivé
 * demandé par son identifiant est traité comme introuvable.
 */
@Service
public class ServiceBlog {

    public record CategorieBlog(Integer id, String nom) {
    }

    private final ArticleRepository articles;
    private final CategorieArticleRepository categories;
    private final AccesAdministrateur acces;
    private final ServiceAudit audit;
    private final Clock horloge;

    public ServiceBlog(ArticleRepository articles, CategorieArticleRepository categories, AccesAdministrateur acces,
                       ServiceAudit audit, Clock horloge) {
        this.articles = articles;
        this.categories = categories;
        this.acces = acces;
        this.audit = audit;
        this.horloge = horloge;
    }

    // ---------- Public ----------

    @Transactional(readOnly = true)
    public Page<ArticleVue> publies(Integer categorieId, Pageable pagination) {
        return articles.parStatut(StatutArticle.publie, categorieId, pagination).map(ArticleVue::resume);
    }

    @Transactional(readOnly = true)
    public ArticleVue publie(Integer id) {
        return articles.findWithDetailsById(id).filter(Article::estPublie).map(ArticleVue::complet)
                .orElseThrow(() -> new RessourceIntrouvableException("Article", id));
    }

    @Transactional(readOnly = true)
    public List<CategorieBlog> categories() {
        return categories.findAll(Sort.by("nom")).stream().map(c -> new CategorieBlog(c.getId(), c.getNom())).toList();
    }

    // ---------- Back-office ----------

    @Transactional(readOnly = true)
    public Page<ArticleVue> tous(Integer administrateurId, StatutArticle statut, Pageable pagination) {
        acces.exiger(administrateurId, AccesAdministrateur.EDITEUR);
        return articles.pourLeBackOffice(statut, pagination).map(ArticleVue::resume);
    }

    @Transactional(readOnly = true)
    public ArticleVue article(Integer administrateurId, Integer id) {
        acces.exiger(administrateurId, AccesAdministrateur.EDITEUR);
        return ArticleVue.complet(charger(id));
    }

    @Transactional
    public ArticleVue creer(Integer administrateurId, RequeteArticle requete, String ip) {
        Administrateur administrateur = acces.exiger(administrateurId, AccesAdministrateur.EDITEUR);
        Article article = articles.save(new Article(administrateur, categorie(requete.categorieId()),
                requete.titre().trim(), requete.contenu().trim()));
        audit.enregistrer(administrateur, "creation_article", "article#" + article.getId(), ip);
        return ArticleVue.complet(article);
    }

    @Transactional
    public ArticleVue modifier(Integer administrateurId, Integer id, RequeteArticle requete, String ip) {
        Administrateur administrateur = acces.exiger(administrateurId, AccesAdministrateur.EDITEUR);
        Article article = charger(id);
        article.setTitre(requete.titre().trim());
        article.setContenu(requete.contenu().trim());
        article.setCategorie(categorie(requete.categorieId()));
        audit.enregistrer(administrateur, "modification_article", "article#" + id, ip);
        return ArticleVue.complet(article);
    }

    @Transactional
    public ArticleVue publier(Integer administrateurId, Integer id, String ip) {
        Administrateur administrateur = acces.exiger(administrateurId, AccesAdministrateur.EDITEUR);
        Article article = charger(id);
        if (!article.estPublie()) {
            article.publier(LocalDate.now(horloge));
            audit.enregistrer(administrateur, "publication_article", "article#" + id, ip);
        }
        return ArticleVue.complet(article);
    }

    @Transactional
    public ArticleVue archiver(Integer administrateurId, Integer id, String ip) {
        Administrateur administrateur = acces.exiger(administrateurId, AccesAdministrateur.EDITEUR);
        Article article = charger(id);
        if (article.getStatut() != StatutArticle.archive) {
            article.archiver();
            audit.enregistrer(administrateur, "archivage_article", "article#" + id, ip);
        }
        return ArticleVue.complet(article);
    }

    @Transactional
    public void supprimer(Integer administrateurId, Integer id, String ip) {
        Administrateur administrateur = acces.exiger(administrateurId, AccesAdministrateur.EDITEUR);
        articles.delete(charger(id));
        audit.enregistrer(administrateur, "suppression_article", "article#" + id, ip);
    }

    private Article charger(Integer id) {
        return articles.findWithDetailsById(id).orElseThrow(() -> new RessourceIntrouvableException("Article", id));
    }

    private CategorieArticle categorie(Integer id) {
        return categories.findById(id).orElseThrow(() -> new DonneeInvalideException("categorieId", "catégorie inconnue"));
    }
}
