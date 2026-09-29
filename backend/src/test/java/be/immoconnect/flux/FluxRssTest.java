package be.immoconnect.flux;

import static org.assertj.core.api.Assertions.assertThat;

import be.immoconnect.config.ConfigurationHorloge;
import be.immoconnect.flux.FluxRss.Canal;
import be.immoconnect.flux.FluxRss.Element;
import be.immoconnect.flux.FluxRss.Piece;
import java.io.StringReader;
import java.time.ZonedDateTime;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/** Test unitaire de l'écriture RSS : le document produit est relu par un analyseur XML. */
class FluxRssTest {

    private static final ZonedDateTime JOUR = ZonedDateTime.of(2026, 9, 28, 0, 0, 0, 0, ConfigurationHorloge.FUSEAU_AGENCE);
    private static final Canal CANAL = new Canal("ImmoConnect — le blog", "https://exemple.be/blog", "Actualité",
            "https://exemple.be/api/v1/flux/articles", JOUR);

    @Test
    void leFluxEstUnDocumentRss2Valide() throws Exception {
        Document flux = lire(FluxRss.ecrire(CANAL, List.of(
                new Element("Acheter à Ixelles", "https://exemple.be/blog/7", "Résumé", "Marché", "Lotte Goossens", JOUR, null))));

        assertThat(flux.getDocumentElement().getTagName()).isEqualTo("rss");
        assertThat(flux.getDocumentElement().getAttribute("version")).isEqualTo("2.0");
        assertThat(texte(flux, "language")).isEqualTo("fr-be");
        assertThat(texte(flux, "lastBuildDate")).isEqualTo("Mon, 28 Sep 2026 00:00:00 +0200");
        assertThat(flux.getElementsByTagName("item").getLength()).isEqualTo(1);
        assertThat(texte(flux, "guid")).isEqualTo("https://exemple.be/blog/7");
        assertThat(texte(flux, "pubDate")).isEqualTo("Mon, 28 Sep 2026 00:00:00 +0200");
        assertThat(texte(flux, "dc:creator")).isEqualTo("Lotte Goossens");
        assertThat(flux.getElementsByTagName("atom:link").item(0).getAttributes().getNamedItem("rel").getNodeValue()).isEqualTo("self");
    }

    @Test
    void unTitreHostileResteDuTexte() throws Exception {
        String titre = "Loft <script>alert('x')</script> & \"terrasse\" ]]>";
        String xml = FluxRss.ecrire(CANAL, List.of(new Element(titre, "https://exemple.be/biens/1?a=1&b=2", titre, null, null, null, null)));

        assertThat(xml).doesNotContain("<script>");
        Document flux = lire(xml);
        // Relu, le titre est exactement celui d'origine : échappé à l'écriture, il n'a créé aucune balise
        assertThat(flux.getElementsByTagName("item").item(0).getFirstChild().getTextContent()).isEqualTo(titre);
        assertThat(flux.getElementsByTagName("script").getLength()).isZero();
        assertThat(texte(flux, "guid")).isEqualTo("https://exemple.be/biens/1?a=1&b=2");
    }

    @Test
    void lesCaracteresInterditsEnXmlSontRetires() throws Exception {
        Document flux = lire(FluxRss.ecrire(CANAL, List.of(
                new Element("Studio\u0000 clair\u0008", "https://exemple.be/biens/2", "Deux\u000Blignes\net un\ttab", null, null, null, null))));

        assertThat(flux.getElementsByTagName("item").item(0).getFirstChild().getTextContent()).isEqualTo("Studio clair");
        assertThat(flux.getElementsByTagName("description").item(1).getTextContent()).isEqualTo("Deuxlignes\net un\ttab");
    }

    @Test
    void laPhotoDeCouvertureEstUnePieceJointe() throws Exception {
        Document flux = lire(FluxRss.ecrire(CANAL, List.of(new Element("Maison", "https://exemple.be/biens/3", "Résumé", "Maison",
                null, JOUR, new Piece("https://exemple.be/storage/biens/3/photo-1.jpg", 120_450, "image/jpeg")))));

        var piece = flux.getElementsByTagName("enclosure").item(0).getAttributes();
        assertThat(piece.getNamedItem("url").getNodeValue()).isEqualTo("https://exemple.be/storage/biens/3/photo-1.jpg");
        assertThat(piece.getNamedItem("length").getNodeValue()).isEqualTo("120450");
        assertThat(piece.getNamedItem("type").getNodeValue()).isEqualTo("image/jpeg");
        assertThat(flux.getElementsByTagName("dc:creator").getLength()).isZero();
    }

    @Test
    void unFluxVideResteValide() throws Exception {
        Document flux = lire(FluxRss.ecrire(CANAL, List.of()));

        assertThat(texte(flux, "title")).isEqualTo("ImmoConnect — le blog");
        assertThat(flux.getElementsByTagName("item").getLength()).isZero();
    }

    static Document lire(String xml) throws Exception {
        DocumentBuilderFactory fabrique = DocumentBuilderFactory.newInstance();
        // Lecture sûre : ni DOCTYPE ni entité externe
        fabrique.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return fabrique.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    static String texte(Document flux, String balise) {
        return flux.getElementsByTagName(balise).item(0).getTextContent();
    }
}
