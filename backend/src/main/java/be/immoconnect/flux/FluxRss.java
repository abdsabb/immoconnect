package be.immoconnect.flux;

import java.io.StringWriter;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Écriture d'un flux RSS 2.0. Le document est produit par l'écrivain XML du JDK, jamais par
 * concaténation de chaînes : un titre contenant « &lt; » ou « &amp; » est échappé, il ne peut ni casser
 * le document ni y injecter une balise.
 */
public final class FluxRss {

    public static final String TYPE = "application/rss+xml;charset=UTF-8";

    private static final String ATOM = "http://www.w3.org/2005/Atom";
    private static final String DUBLIN_CORE = "http://purl.org/dc/elements/1.1/";
    /** Dates au format de la RFC 822, celui qu'exige RSS. */
    private static final DateTimeFormatter DATE = DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.ENGLISH);

    /** @param adresse adresse du flux lui-même, reprise dans le lien « self » */
    public record Canal(String titre, String lien, String description, String adresse, ZonedDateTime misAJour) {
    }

    /** Fichier joint à un élément : la photo de couverture d'un bien. */
    public record Piece(String adresse, long taille, String type) {
    }

    /** @param lien adresse de la page, qui sert aussi d'identifiant permanent de l'élément */
    public record Element(String titre, String lien, String description, String categorie, String auteur,
                          ZonedDateTime publieLe, Piece piece) {
    }

    private FluxRss() {
    }

    public static String ecrire(Canal canal, List<Element> elements) {
        StringWriter sortie = new StringWriter();
        try {
            XMLStreamWriter xml = XMLOutputFactory.newFactory().createXMLStreamWriter(sortie);
            xml.writeStartDocument("UTF-8", "1.0");
            xml.writeStartElement("rss");
            xml.writeAttribute("version", "2.0");
            xml.writeNamespace("atom", ATOM);
            xml.writeNamespace("dc", DUBLIN_CORE);
            xml.writeStartElement("channel");
            balise(xml, "title", canal.titre());
            balise(xml, "link", canal.lien());
            balise(xml, "description", canal.description());
            balise(xml, "language", "fr-be");
            balise(xml, "lastBuildDate", DATE.format(canal.misAJour()));
            balise(xml, "ttl", "60");
            xml.writeEmptyElement(ATOM, "link");
            xml.writeAttribute("href", canal.adresse());
            xml.writeAttribute("rel", "self");
            xml.writeAttribute("type", "application/rss+xml");
            for (Element element : elements) {
                ecrire(xml, element);
            }
            xml.writeEndElement();
            xml.writeEndElement();
            xml.writeEndDocument();
            xml.close();
        } catch (XMLStreamException e) {
            throw new IllegalStateException("Flux RSS impossible à écrire", e);
        }
        return sortie.toString();
    }

    private static void ecrire(XMLStreamWriter xml, Element element) throws XMLStreamException {
        xml.writeStartElement("item");
        balise(xml, "title", element.titre());
        balise(xml, "link", element.lien());
        xml.writeStartElement("guid");
        xml.writeAttribute("isPermaLink", "true");
        xml.writeCharacters(element.lien());
        xml.writeEndElement();
        balise(xml, "description", element.description());
        if (element.categorie() != null) {
            balise(xml, "category", element.categorie());
        }
        if (element.auteur() != null) {
            // La balise « author » de RSS exige une adresse e-mail : le nom seul va dans dc:creator
            xml.writeStartElement(DUBLIN_CORE, "creator");
            xml.writeCharacters(lisible(element.auteur()));
            xml.writeEndElement();
        }
        if (element.publieLe() != null) {
            balise(xml, "pubDate", DATE.format(element.publieLe()));
        }
        if (element.piece() != null) {
            xml.writeEmptyElement("enclosure");
            xml.writeAttribute("url", element.piece().adresse());
            xml.writeAttribute("length", String.valueOf(element.piece().taille()));
            xml.writeAttribute("type", element.piece().type());
        }
        xml.writeEndElement();
    }

    private static void balise(XMLStreamWriter xml, String nom, String texte) throws XMLStreamException {
        xml.writeStartElement(nom);
        xml.writeCharacters(lisible(texte));
        xml.writeEndElement();
    }

    /** XML 1.0 interdit les caractères de contrôle, même échappés : ils sont retirés. */
    static String lisible(String texte) {
        return texte == null ? "" : texte.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\uFFFE\\uFFFF]", "");
    }
}
