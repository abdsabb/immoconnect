package be.immoconnect.stockage;

import be.immoconnect.service.DonneeInvalideException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;

/**
 * Contrôle et normalisation d'une photo téléversée (livrable 16, §4.2).
 * <ol>
 *   <li>Le type est établi en lisant le contenu du fichier : ni son nom ni le type annoncé par le
 *       navigateur ne font foi.</li>
 *   <li>Les dimensions sont lues avant le décodage, pour refuser une image piégée qui occuperait
 *       toute la mémoire une fois décompressée.</li>
 *   <li>L'image est toujours ré-encodée en JPEG : seuls les pixels survivent. Un contenu actif caché
 *       dans le fichier disparaît, ainsi que les métadonnées EXIF — dont la position GPS de la prise de vue.</li>
 * </ol>
 */
@Component
public class ImagesBiens {

    private static final Set<String> FORMATS_ACCEPTES = Set.of("jpeg", "png");
    private static final int COTE_MAX = 1600;
    private static final int COTE_MIN = 400;
    private static final long PIXELS_MAX = 40_000_000L;
    private static final double QUALITE_JPEG = 0.85;

    public byte[] normaliser(byte[] fichier) {
        BufferedImage image = lire(fichier);
        try (ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            var conversion = Thumbnails.of(image);
            if (image.getWidth() > COTE_MAX || image.getHeight() > COTE_MAX) {
                conversion.size(COTE_MAX, COTE_MAX);
            } else {
                conversion.scale(1.0);
            }
            // TYPE_INT_RGB : le JPEG n'a pas de transparence, celle d'un PNG est aplatie
            conversion.imageType(BufferedImage.TYPE_INT_RGB)
                    .outputFormat("jpg")
                    .outputQuality(QUALITE_JPEG)
                    .toOutputStream(sortie);
            return sortie.toByteArray();
        } catch (IOException e) {
            throw invalide("la photo n'a pas pu être traitée");
        }
    }

    private static BufferedImage lire(byte[] fichier) {
        if (fichier == null || fichier.length == 0) {
            throw invalide("le fichier est vide");
        }
        try (ImageInputStream flux = ImageIO.createImageInputStream(new ByteArrayInputStream(fichier))) {
            Iterator<ImageReader> lecteurs = ImageIO.getImageReaders(flux);
            if (!lecteurs.hasNext()) {
                throw invalide("le fichier n'est pas une image");
            }
            ImageReader lecteur = lecteurs.next();
            try {
                if (!FORMATS_ACCEPTES.contains(lecteur.getFormatName().toLowerCase())) {
                    throw invalide("seules les photos JPEG et PNG sont acceptées");
                }
                lecteur.setInput(flux);
                int largeur = lecteur.getWidth(0);
                int hauteur = lecteur.getHeight(0);
                if ((long) largeur * hauteur > PIXELS_MAX) {
                    throw invalide("la photo est trop grande (40 mégapixels au plus)");
                }
                if (Math.max(largeur, hauteur) < COTE_MIN) {
                    throw invalide("la photo est trop petite (" + COTE_MIN + " pixels de côté au moins)");
                }
                return lecteur.read(0);
            } finally {
                lecteur.dispose();
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof DonneeInvalideException refus) {
                throw refus;
            }
            throw invalide("le fichier n'est pas une image lisible");
        }
    }

    private static DonneeInvalideException invalide(String message) {
        return new DonneeInvalideException("fichier", message);
    }
}
