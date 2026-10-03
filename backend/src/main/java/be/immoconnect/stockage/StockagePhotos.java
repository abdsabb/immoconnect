package be.immoconnect.stockage;

/**
 * Stockage des photos des biens et des couvertures d'articles, vu par l'application (pattern Adapter).
 * Les services ignorent où vont les fichiers : passer du disque à un stockage objet ne toucherait que l'adaptateur.
 */
public interface StockagePhotos {

    /** Préfixe public des photos : l'URL enregistrée en base commence toujours par lui. */
    String PREFIXE_URL = "/storage/";

    /**
     * @param contenu image JPEG déjà validée et ré-encodée
     * @return l'URL publique de la photo, à enregistrer en base
     */
    String enregistrer(Integer bienId, byte[] contenu);

    /**
     * @param contenu image JPEG déjà validée et ré-encodée
     * @return l'URL publique de l'image de couverture de l'article, à enregistrer en base
     */
    String enregistrerCouverture(Integer articleId, byte[] contenu);

    /** Sans effet si le fichier n'existe pas (photos des données de test). */
    void supprimer(String url);
}
