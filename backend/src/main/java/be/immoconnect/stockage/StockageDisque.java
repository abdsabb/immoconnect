package be.immoconnect.stockage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stockage sur le disque du serveur (un volume Docker en production), sous
 * {dossier}/biens/{identifiant du bien}/{nom aléatoire}.jpg et {dossier}/articles/{identifiant de l'article}/….
 * Le nom du fichier est généré par le serveur : rien de ce que l'utilisateur a saisi n'entre
 * dans le chemin, ce qui écarte toute remontée de répertoire.
 */
public class StockageDisque implements StockagePhotos {

    private static final Logger journal = LoggerFactory.getLogger(StockageDisque.class);

    private final Path racine;

    public StockageDisque(Path racine) {
        this.racine = racine.toAbsolutePath().normalize();
    }

    public Path racine() {
        return racine;
    }

    @Override
    public String enregistrer(Integer bienId, byte[] contenu) {
        return ecrire("biens/" + bienId, contenu);
    }

    @Override
    public String enregistrerCouverture(Integer articleId, byte[] contenu) {
        return ecrire("articles/" + articleId, contenu);
    }

    private String ecrire(String dossier, byte[] contenu) {
        String relatif = dossier + "/" + UUID.randomUUID() + ".jpg";
        Path fichier = racine.resolve(relatif);
        try {
            Files.createDirectories(fichier.getParent());
            Files.write(fichier, contenu);
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible d'enregistrer la photo", e);
        }
        return PREFIXE_URL + relatif;
    }

    @Override
    public void supprimer(String url) {
        if (url == null || !url.startsWith(PREFIXE_URL)) {
            return;
        }
        Path fichier = racine.resolve(url.substring(PREFIXE_URL.length())).normalize();
        if (!fichier.startsWith(racine)) {
            return;
        }
        try {
            Files.deleteIfExists(fichier);
        } catch (IOException e) {
            // La ligne en base est supprimée de toute façon : un fichier orphelin n'est qu'un peu d'espace perdu
            journal.warn("Photo {} non supprimée du disque : {}", url, e.getMessage());
        }
    }
}
