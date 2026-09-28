package be.immoconnect.stockage;

import be.immoconnect.depot.PhotoRepository;
import be.immoconnect.entite.Photo;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * Photos des données de test. Les annonces chargées par Flyway référencent des fichiers
 * /storage/biens/{id}/photo-{n}.jpg qu'aucun serveur neuf ne possède : au démarrage, chaque fichier
 * manquant est créé à partir d'un jeu de photos sous licence libre embarqué dans l'application
 * (auteurs et licences : photos-demo/CREDITS.md). La photo est choisie d'après la légende.
 * Une photo téléversée par un agent porte un nom aléatoire : elle n'est jamais concernée.
 */
@Component
@ConditionalOnProperty(name = "immoconnect.demonstration.photos", havingValue = "true")
public class PhotosDeDemonstration implements ApplicationRunner {

    private static final Logger journal = LoggerFactory.getLogger(PhotosDeDemonstration.class);
    private static final String CATALOGUE = "classpath:/photos-demo/*.jpg";
    private static final Pattern PHOTO_DE_TEST = Pattern.compile("^/storage/(biens/(\\d+)/photo-\\d+\\.jpg)$");
    private static final String THEME_PAR_DEFAUT = "sejour";

    /** Légende des données de test → thème du catalogue. */
    private static final Map<String, String> THEMES = Map.ofEntries(
            Map.entry("Façade", "facade"),
            Map.entry("Séjour", "sejour"),
            Map.entry("Cuisine", "cuisine"),
            Map.entry("Chambre principale", "chambre"),
            Map.entry("Chambre 2", "chambre"),
            Map.entry("Salle de bains", "bains"),
            Map.entry("Jardin", "jardin"),
            Map.entry("Terrasse", "terrasse"),
            Map.entry("Hall d'entrée", "hall"),
            Map.entry("Vue depuis le salon", "vue"),
            Map.entry("Plan du bien", "plan"),
            Map.entry("Terrain", "terrain"),
            Map.entry("Garage", "garage"),
            Map.entry("Espace commercial", "commerce"));

    /** Sans légende, la position décide : la couverture montre l'extérieur, la suite l'intérieur. */
    private static final List<String> THEMES_SANS_LEGENDE = List.of("facade", "sejour", "cuisine", "chambre", "bains", "terrasse");

    private final PhotoRepository photos;
    private final Path racine;

    public PhotosDeDemonstration(PhotoRepository photos, StockageDisque stockage) {
        this.photos = photos;
        this.racine = stockage.racine();
    }

    @Override
    public void run(ApplicationArguments arguments) {
        try {
            int creees = installer(charger());
            if (creees > 0) {
                journal.info("Photos de démonstration : {} fichiers créés dans {}", creees, racine);
            }
        } catch (IOException | UncheckedIOException e) {
            // Des photos absentes n'empêchent pas le site de fonctionner : il affiche l'image de remplacement
            journal.warn("Photos de démonstration non installées : {}", e.getMessage());
        }
    }

    /** @return le nombre de fichiers créés */
    int installer(Map<String, List<Resource>> catalogue) throws IOException {
        if (catalogue.isEmpty()) {
            return 0;
        }
        int creees = 0;
        for (Photo photo : photos.findByUrlStartingWith(StockagePhotos.PREFIXE_URL)) {
            Matcher url = PHOTO_DE_TEST.matcher(photo.getUrl());
            if (!url.matches()) {
                continue;
            }
            Path fichier = racine.resolve(url.group(1)).normalize();
            if (!fichier.startsWith(racine) || Files.exists(fichier)) {
                continue;
            }
            List<Resource> choix = catalogue.getOrDefault(theme(photo), catalogue.get(THEME_PAR_DEFAUT));
            if (choix == null) {
                continue;
            }
            // Deux photos du même bien diffèrent par leur position : elles reçoivent deux images différentes
            int bienId = Integer.parseInt(url.group(2));
            Resource image = choix.get(Math.floorMod(bienId * 3 + photo.getOrdre(), choix.size()));
            Files.createDirectories(fichier.getParent());
            try (InputStream contenu = image.getInputStream()) {
                Files.copy(contenu, fichier);
            }
            creees++;
        }
        return creees;
    }

    /** Catalogue embarqué : photos-demo/{thème}-{numéro}.jpg, regroupées par thème. */
    static Map<String, List<Resource>> charger() throws IOException {
        Map<String, List<Resource>> catalogue = new TreeMap<>();
        Resource[] images = new PathMatchingResourcePatternResolver().getResources(CATALOGUE);
        for (Resource image : images) {
            String nom = image.getFilename();
            if (nom != null && nom.contains("-")) {
                catalogue.computeIfAbsent(nom.substring(0, nom.lastIndexOf('-')), t -> new ArrayList<>()).add(image);
            }
        }
        catalogue.values().forEach(liste -> liste.sort(Comparator.comparing(Resource::getFilename)));
        return catalogue;
    }

    private static String theme(Photo photo) {
        if (photo.getLegende() == null) {
            return THEMES_SANS_LEGENDE.get(Math.floorMod(photo.getOrdre() - 1, THEMES_SANS_LEGENDE.size()));
        }
        return THEMES.getOrDefault(photo.getLegende(), THEME_PAR_DEFAUT);
    }
}
