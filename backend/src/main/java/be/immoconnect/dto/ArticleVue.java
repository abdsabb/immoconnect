package be.immoconnect.dto;

import be.immoconnect.entities.Article;
import be.immoconnect.entities.StatutArticle;
import java.time.LocalDate;

/**
 * Article du blog. Dans une liste, {@code contenu} est absent et {@code extrait} en donne le début ;
 * sur la page de l'article, le contenu est complet. Le contenu est du texte brut : aucune balise
 * n'est interprétée à l'affichage.
 */
public record ArticleVue(Integer id, String titre, String extrait, String contenu, Integer categorieId, String categorie,
                         StatutArticle statut, LocalDate publieLe, String auteur) {

    private static final int LONGUEUR_EXTRAIT = 220;

    public static ArticleVue resume(Article article) {
        return depuis(article, null);
    }

    public static ArticleVue complet(Article article) {
        return depuis(article, article.getContenu());
    }

    private static ArticleVue depuis(Article article, String contenu) {
        String texte = article.getContenu();
        String extrait = texte.length() <= LONGUEUR_EXTRAIT ? texte : texte.substring(0, LONGUEUR_EXTRAIT - 1).stripTrailing() + "…";
        return new ArticleVue(article.getId(), article.getTitre(), extrait, contenu, article.getCategorie().getId(),
                article.getCategorie().getNom(), article.getStatut(), article.getPublieLe(),
                article.getAdministrateur().getNomComplet());
    }
}
