package be.immoconnect.stockage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import be.immoconnect.entities.Photo;
import be.immoconnect.repositories.PhotoRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/** Test unitaire de l'installation des photos de démonstration, sur un dossier temporaire. */
class PhotosDeDemonstrationTest {

    @TempDir
    Path dossier;

    private final PhotoRepository photos = mock(PhotoRepository.class);

    private final Map<String, List<Resource>> catalogue = Map.of(
            "facade", List.of(image("facade-01"), image("facade-02")),
            "sejour", List.of(image("sejour-01")),
            "chambre", List.of(image("chambre-01"), image("chambre-02"), image("chambre-03")));

    @Test
    void laPhotoManquanteEstCreeeDApresSaLegende() throws IOException {
        enBase(photo("/storage/biens/4/photo-1.jpg", 1, "Façade"));

        assertThat(installer()).isEqualTo(1);
        assertThat(contenu("biens/4/photo-1.jpg")).startsWith("facade-");
    }

    @Test
    void deuxChambresDuMemeBienRecoiventDeuxImagesDifferentes() throws IOException {
        enBase(photo("/storage/biens/9/photo-2.jpg", 2, "Chambre principale"),
                photo("/storage/biens/9/photo-3.jpg", 3, "Chambre 2"));

        installer();

        assertThat(contenu("biens/9/photo-2.jpg")).startsWith("chambre-")
                .isNotEqualTo(contenu("biens/9/photo-3.jpg"));
    }

    @Test
    void sansLegendeLaCouvertureMontreLExterieur() throws IOException {
        enBase(photo("/storage/biens/5/photo-1.jpg", 1, null));

        installer();

        assertThat(contenu("biens/5/photo-1.jpg")).startsWith("facade-");
    }

    @Test
    void uneLegendeInconnueOuUnThemeAbsentDonneUnSejour() throws IOException {
        enBase(photo("/storage/biens/6/photo-1.jpg", 1, "Cave à vin"),
                photo("/storage/biens/6/photo-2.jpg", 2, "Cuisine"));

        installer();

        assertThat(contenu("biens/6/photo-1.jpg")).isEqualTo("sejour-01");
        assertThat(contenu("biens/6/photo-2.jpg")).isEqualTo("sejour-01");
    }

    @Test
    void unFichierExistantNEstJamaisRemplace() throws IOException {
        Files.createDirectories(dossier.resolve("biens/4"));
        Files.writeString(dossier.resolve("biens/4/photo-1.jpg"), "déjà là");
        enBase(photo("/storage/biens/4/photo-1.jpg", 1, "Façade"));

        assertThat(installer()).isZero();
        assertThat(contenu("biens/4/photo-1.jpg")).isEqualTo("déjà là");
    }

    @Test
    void laPhotoTeleverseeParUnAgentEstIgnoree() throws IOException {
        enBase(photo("/storage/biens/4/0b9c1f0e-7d0c-4f7e-9a53-0c2d7f1f4a11.jpg", 1, "Façade"),
                photo("/storage/../application.yml", 2, "Façade"));

        assertThat(installer()).isZero();
        assertThat(dossier).isEmptyDirectory();
    }

    @Test
    void leCatalogueEmbarqueCouvreTousLesThemes() throws IOException {
        Map<String, List<Resource>> embarque = PhotosDeDemonstration.charger();

        assertThat(embarque.keySet()).contains("facade", "sejour", "cuisine", "chambre", "bains", "jardin",
                "terrasse", "hall", "vue", "plan", "terrain", "garage", "commerce");
        // Un bien compte six photos au plus : six images par thème évitent tout doublon dans une galerie
        assertThat(embarque.values()).allSatisfy(images -> assertThat(images).hasSizeGreaterThanOrEqualTo(6));
    }

    private int installer() throws IOException {
        return new PhotosDeDemonstration(photos, new StockageDisque(dossier)).installer(catalogue);
    }

    private void enBase(Photo... lignes) {
        when(photos.findByUrlStartingWith(StockagePhotos.PREFIXE_URL)).thenReturn(List.of(lignes));
    }

    private String contenu(String relatif) throws IOException {
        return Files.readString(dossier.resolve(relatif));
    }

    private static Photo photo(String url, int ordre, String legende) {
        return new Photo(null, url, ordre, legende);
    }

    /** Fausse image dont le contenu est le nom : le test lit le fichier pour savoir laquelle a été choisie. */
    private static Resource image(String nom) {
        return new ByteArrayResource(nom.getBytes()) {
            @Override
            public String getFilename() {
                return nom + ".jpg";
            }
        };
    }
}
