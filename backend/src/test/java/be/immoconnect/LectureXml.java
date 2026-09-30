package be.immoconnect;

import java.io.StringReader;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/** Relecture d'un document XML produit par l'application, pour les tests des flux RSS. */
public final class LectureXml {

    private LectureXml() {
    }

    public static Document lire(String xml) throws Exception {
        DocumentBuilderFactory fabrique = DocumentBuilderFactory.newInstance();
        // Lecture sûre : ni DOCTYPE ni entité externe
        fabrique.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return fabrique.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }
}
