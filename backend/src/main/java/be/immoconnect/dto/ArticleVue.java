package be.immoconnect.dto;

import be.immoconnect.entities.Article;
import be.immoconnect.entities.StatutArticle;
import java.time.LocalDate;

/**
 * Article du blog. Dans une liste, {@code contenu} est absent et {@code extrait} en donne le début ;
 * sur la page de l'article, le contenu est complet. Le contenu est du texte brut : aucune balise
 * n'est interprétée à l'affichage. Deux marques de mise en forme seulement, en début de ligne :
 * « ## » pour un intertitre, « - » pour un élément de liste.
 */
public record ArticleVue(Integer id, String titre, String extrait, String contenu, String imageUrl, int minutesDeLecture,
                         Integer categorieId, String categorie, StatutArticle statut, LocalDate publieLe, String auteur) {

    private static final int LONGUEUR_EXTRAIT = 220;
    private static final int MOTS_PAR_MINUTE = 200;

    public static ArticleVue resume(Article article) {
        return depuis(article, null);
    }

    public static ArticleVue complet(Article article) {
        return depuis(article, article.getContenu());
    }

    /** Le texte sans ses marques de mise en forme, pour un extrait ou un flux RSS. */
    public static String sansMarques(String contenu) {
        return contenu.replaceAll("(?m)^(## |- )", "");
    }

    private static ArticleVue depuis(Article article, String contenu) {
        String texte = article.getContenu();
        return new ArticleVue(article.getId(), article.getTitre(), extrait(texte), contenu, article.getImageUrl(), minutes(texte),
                article.getCategorie().getId(), article.getCategorie().getNom(), article.getStatut(), article.getPublieLe(),
                article.getAdministrateur().getNomComplet());
    }

    /** Le chapeau de l'article : son premier paragraphe, coupé s'il est long. */
    private static String extrait(String texte) {
        String chapeau = sansMarques(texte.strip()).split("\\n\\s*\\n", 2)[0].replaceAll("\\s*\\n\\s*", " ");
        return chapeau.length() <= LONGUEUR_EXTRAIT ? chapeau : chapeau.substring(0, LONGUEUR_EXTRAIT - 1).stripTrailing() + "…";
    }

    private static int minutes(String texte) {
        int mots = texte.isBlank() ? 0 : texte.strip().split("\\s+").length;
        return Math.max(1, (int) Math.ceil(mots / (double) MOTS_PAR_MINUTE));
    }
}
