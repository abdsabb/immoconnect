package be.immoconnect.services;

import be.immoconnect.dto.ArticleVue;
import be.immoconnect.dto.BienDetail;
import be.immoconnect.dto.BienResume;
import be.immoconnect.dto.CritereRechercheBien;
import be.immoconnect.dto.SiteInfos;
import be.immoconnect.entities.TypeOffre;
import be.immoconnect.exceptions.RessourceIntrouvableException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Rendu côté serveur des pages publiques (chapitre 10 du rapport) : accueil, listes, fiches de biens et
 * blog. Le robot d'indexation, ou le visiteur qui arrive sur l'adresse, reçoit un HTML complet — titre,
 * description, hreflang, canonique, données Schema.org et contenu visible — sur lequel l'application
 * React prend ensuite le relais. Les espaces connectés restent rendus par le navigateur et exclus de
 * l'indexation (robots.txt).
 * <p>
 * Pas de transaction ici : chaque service appelé ouvre la sienne. Un bien ou un article introuvable y
 * lève une exception que ce service rattrape pour répondre « page inconnue » — dans une transaction
 * commune, elle l'aurait condamnée à l'annulation.
 */
@Service
public class ServiceRenduPublic {

    public record Page_(String titre, String description, String chemin, String langue, String jsonLd, String corps,
                        boolean indexable) {
    }

    public static final List<String> LANGUES = List.of("fr", "nl", "en");
    private static final int BIENS_PAR_PAGE = 12;
    private static final int ARTICLES_PAR_PAGE = 20;
    private static final Pattern NUMERO = Pattern.compile("^\\d{1,9}$");

    private static final Map<String, Map<String, String>> TEXTES = Map.of(
            "fr", texte("accueil", "Accueil", "vente", "À vendre", "location", "À louer", "biens", "Tous nos biens", "blog", "Blog",
                    "chambres", "chambres", "mois", "/ mois", "voir", "Voir la fiche", "publie", "Publié le", "agent", "Votre agent",
                    "description", "Description", "localisation", "Localisation", "quartier", "L'adresse exacte est communiquée après la prise de rendez-vous.",
                    "accueilTitre", "Trouvez votre prochain chez-vous à Bruxelles", "accueilSousTitre", "Appartements, maisons et commerces à vendre et à louer",
                    "derniers", "Nos dernières annonces", "listeDesc", "annonces immobilières avec photos, prix, classe PEB et carte du quartier. Visite en ligne, en journée gratuite, en soirée et le week-end.",
                    "blogDesc", "Conseils pour acheter, vendre et louer à Bruxelles : guides, fiscalité, rénovation et PEB.", "rechercher", "Rechercher un bien", "toutesLangues", "Ce site existe en français, néerlandais et anglais."),
            "nl", texte("accueil", "Home", "vente", "Te koop", "location", "Te huur", "biens", "Al onze panden", "blog", "Blog",
                    "chambres", "slaapkamers", "mois", "/ maand", "voir", "Bekijk het pand", "publie", "Gepubliceerd op", "agent", "Uw makelaar",
                    "description", "Beschrijving", "localisation", "Ligging", "quartier", "Het exacte adres wordt meegedeeld na het maken van een afspraak.",
                    "accueilTitre", "Vind uw volgende thuis in Brussel", "accueilSousTitre", "Appartementen, huizen en handelspanden te koop en te huur",
                    "derniers", "Onze laatste advertenties", "listeDesc", "vastgoedadvertenties met foto's, prijs, EPC-klasse en buurtkaart. Bezoek online, overdag gratis, 's avonds en in het weekend.",
                    "blogDesc", "Advies om te kopen, verkopen en huren in Brussel: gidsen, fiscaliteit, renovatie en EPC.", "rechercher", "Een pand zoeken", "toutesLangues", "Deze site bestaat in het Frans, Nederlands en Engels."),
            "en", texte("accueil", "Home", "vente", "For sale", "location", "For rent", "biens", "All our properties", "blog", "Blog",
                    "chambres", "bedrooms", "mois", "/ month", "voir", "View the listing", "publie", "Published on", "agent", "Your agent",
                    "description", "Description", "localisation", "Location", "quartier", "The exact address is given once a viewing is booked.",
                    "accueilTitre", "Find your next home in Brussels", "accueilSousTitre", "Flats, houses and shops for sale and for rent",
                    "derniers", "Our latest listings", "listeDesc", "property listings with photos, price, EPC rating and neighbourhood map. Book a viewing online, free in the daytime, evenings and weekends too.",
                    "blogDesc", "Advice on buying, selling and renting in Brussels: guides, tax, renovation and EPC.", "rechercher", "Search a property", "toutesLangues", "This site is available in French, Dutch and English."));

    private final BienService biens;
    private final ServiceBlog blog;
    private final ServiceParametres parametres;

    public ServiceRenduPublic(BienService biens, ServiceBlog blog, ServiceParametres parametres) {
        this.biens = biens;
        this.blog = blog;
        this.parametres = parametres;
    }

    /** Langues proposées par le site, réglées par l'administrateur (cas A5) : seules celles-là sont annoncées aux moteurs. */
    public List<String> languesActives() {
        List<String> actives = parametres.site().languesActives();
        return actives.isEmpty() ? List.of("fr") : actives;
    }

    /** @return la page pour ce chemin, ou null si le chemin n'est pas une page publique rendue ici */
    public Page_ rendre(String chemin, String langue) {
        String l = LANGUES.contains(langue) ? langue : "fr";
        String c = chemin == null || chemin.isBlank() ? "/" : chemin.replaceAll("/+$", "");
        if (c.isEmpty()) {
            c = "/";
        }
        return switch (c) {
            case "/" -> accueil(l);
            case "/a-vendre" -> liste(l, TypeOffre.vente, c);
            case "/a-louer" -> liste(l, TypeOffre.location, c);
            case "/biens" -> liste(l, null, c);
            case "/blog" -> articles(l);
            default -> {
                if (c.startsWith("/biens/") && NUMERO.matcher(c.substring(7)).matches()) {
                    yield bien(l, Integer.parseInt(c.substring(7)));
                }
                if (c.startsWith("/blog/") && NUMERO.matcher(c.substring(6)).matches()) {
                    yield article(l, Integer.parseInt(c.substring(6)));
                }
                yield null;
            }
        };
    }

    /** Plan du site : pages publiques, biens disponibles et articles publiés, avec leurs versions linguistiques. */
    public List<String> cheminsDuPlan() {
        List<String> chemins = new ArrayList<>(List.of("/", "/a-vendre", "/a-louer", "/biens", "/blog"));
        biens.rechercher(new CritereRechercheBien(null, null, null, null, null, null, null, null), PageRequest.of(0, 1000))
                .forEach(b -> chemins.add("/biens/" + b.id()));
        blog.publies(null, PageRequest.of(0, 1000)).forEach(a -> chemins.add("/blog/" + a.id()));
        return chemins;
    }

    private Page_ accueil(String l) {
        SiteInfos site = parametres.site();
        Map<String, String> t = TEXTES.get(l);
        Page<BienResume> derniers = biens.rechercher(new CritereRechercheBien(null, null, null, null, null, null, null, null),
                PageRequest.of(0, 6, Sort.by(Sort.Direction.DESC, "publieLe", "id")));
        StringBuilder corps = new StringBuilder();
        corps.append("<section><h1>").append(e(t.get("accueilTitre"))).append("</h1><p>").append(e(t.get("accueilSousTitre"))).append("</p>")
                .append("<p><a href=\"/a-vendre\">").append(e(t.get("vente"))).append("</a> · <a href=\"/a-louer\">").append(e(t.get("location")))
                .append("</a> · <a href=\"/blog\">").append(e(t.get("blog"))).append("</a></p></section>");
        corps.append("<section><h2>").append(e(t.get("derniers"))).append("</h2>").append(cartes(derniers.getContent(), t)).append("</section>");
        corps.append("<section><h2>").append(e(site.nom())).append("</h2><p>").append(e(site.slogan())).append("</p><address>")
                .append(e(site.adresse())).append(" · ").append(e(site.telephone())).append(" · ").append(e(site.email())).append("</address><p>")
                .append(e(site.horaires())).append("</p></section>");
        String jsonLd = "{\"@context\":\"https://schema.org\",\"@type\":\"RealEstateAgent\",\"name\":" + j(site.nom())
                + ",\"description\":" + j(site.slogan()) + ",\"address\":" + j(site.adresse()) + ",\"telephone\":" + j(site.telephone())
                + ",\"email\":" + j(site.email()) + ",\"openingHours\":" + j(site.horaires()) + ",\"areaServed\":\"Bruxelles\"}";
        return new Page_(site.nom() + " — " + t.get("accueilTitre"), t.get("accueilSousTitre") + ". " + site.slogan() + ".", "/", l, jsonLd, corps.toString(), true);
    }

    private Page_ liste(String l, TypeOffre type, String chemin) {
        Map<String, String> t = TEXTES.get(l);
        String titre = type == null ? t.get("biens") : t.get(type.name());
        Page<BienResume> page = biens.rechercher(new CritereRechercheBien(type, null, null, null, null, null, null, null),
                PageRequest.of(0, BIENS_PAR_PAGE, Sort.by(Sort.Direction.DESC, "publieLe", "id")));
        String corps = "<section><h1>" + e(titre) + " (" + page.getTotalElements() + ")</h1>" + cartes(page.getContent(), t) + "</section>";
        String jsonLd = "{\"@context\":\"https://schema.org\",\"@type\":\"ItemList\",\"name\":" + j(titre) + ",\"numberOfItems\":" + page.getTotalElements()
                + ",\"itemListElement\":[" + String.join(",", page.getContent().stream().map(b -> "{\"@type\":\"ListItem\",\"position\":"
                + (page.getContent().indexOf(b) + 1) + ",\"url\":\"/biens/" + b.id() + "\",\"name\":" + j(b.titre()) + "}").toList()) + "]}";
        return new Page_(titre + " — " + parametres.site().nom(), page.getTotalElements() + " " + t.get("listeDesc"), chemin, l, jsonLd, corps, true);
    }

    private Page_ bien(String l, Integer id) {
        Map<String, String> t = TEXTES.get(l);
        BienDetail b;
        try {
            b = biens.detail(id, false);
        } catch (RessourceIntrouvableException ex) {
            return null;
        }
        StringBuilder corps = new StringBuilder("<article><h1>").append(e(b.titre())).append("</h1><p>")
                .append(e(t.get(b.typeOffre().name()))).append(" · ").append(e(b.categorie().nom())).append(" · ").append(e(b.codePostal())).append(' ').append(e(b.ville()))
                .append("</p><p><strong>").append(prix(b.typeOffre(), b.prix(), t, l)).append("</strong></p><ul><li>").append(b.superficie().stripTrailingZeros().toPlainString())
                .append(" m²</li><li>").append(b.nbChambres()).append(' ').append(e(t.get("chambres"))).append("</li><li>PEB ").append(e(b.peb().etiquette()))
                .append("</li><li>").append(e(t.get("publie"))).append(' ').append(b.publieLe()).append("</li></ul>");
        for (BienDetail.Photo p : b.photos()) {
            corps.append("<img src=\"").append(e(p.url())).append("\" alt=\"").append(e(p.legende() == null ? b.titre() : p.legende() + " — " + b.titre())).append("\" loading=\"lazy\">");
        }
        corps.append("<h2>").append(e(t.get("description"))).append("</h2><p>").append(e(b.description())).append("</p><h2>").append(e(t.get("localisation")))
                .append("</h2><p>").append(e(t.get("quartier"))).append("</p><p>").append(e(t.get("agent"))).append(" : ").append(e(b.agent().nomComplet()))
                .append(" · ").append(e(b.agent().telephonePro())).append("</p></article>");
        String typeLogement = switch (b.categorie().nom().toLowerCase(Locale.ROOT)) {
            case "maison", "villa" -> "House";
            case "appartement", "studio", "duplex", "loft", "penthouse" -> "Apartment";
            default -> "Accommodation";
        };
        String jsonLd = "{\"@context\":\"https://schema.org\",\"@type\":\"RealEstateListing\",\"name\":" + j(b.titre()) + ",\"description\":" + j(b.description())
                + ",\"url\":\"/biens/" + b.id() + "\",\"datePosted\":\"" + b.publieLe() + "\",\"image\":[" + String.join(",", b.photos().stream().map(p -> j(p.url())).toList()) + "]"
                + ",\"about\":{\"@type\":\"" + typeLogement + "\",\"numberOfRooms\":" + b.nbChambres() + ",\"floorSize\":{\"@type\":\"QuantitativeValue\",\"value\":"
                + b.superficie().stripTrailingZeros().toPlainString() + ",\"unitCode\":\"MTK\"},\"address\":{\"@type\":\"PostalAddress\",\"addressLocality\":" + j(b.ville())
                + ",\"postalCode\":" + j(b.codePostal()) + ",\"addressCountry\":\"BE\"},\"geo\":{\"@type\":\"GeoCoordinates\",\"latitude\":" + b.latitude() + ",\"longitude\":" + b.longitude() + "}}"
                + ",\"offers\":{\"@type\":\"Offer\",\"price\":" + b.prix().stripTrailingZeros().toPlainString() + ",\"priceCurrency\":\"EUR\",\"availability\":\"https://schema.org/InStock\""
                + (b.typeOffre() == TypeOffre.location ? ",\"priceSpecification\":{\"@type\":\"UnitPriceSpecification\",\"price\":" + b.prix().stripTrailingZeros().toPlainString()
                + ",\"priceCurrency\":\"EUR\",\"unitCode\":\"MON\"}" : "") + "}}";
        String description = b.typeOffre() == TypeOffre.vente ? t.get("vente") : t.get("location");
        description += " — " + b.categorie().nom() + " " + b.superficie().stripTrailingZeros().toPlainString() + " m², " + b.nbChambres() + " " + t.get("chambres")
                + ", PEB " + b.peb().etiquette() + ", " + b.ville() + ". " + prix(b.typeOffre(), b.prix(), t, l) + ".";
        return new Page_(b.titre() + " — " + parametres.site().nom(), description, "/biens/" + b.id(), l, jsonLd, corps.toString(), true);
    }

    private Page_ articles(String l) {
        Map<String, String> t = TEXTES.get(l);
        Page<ArticleVue> page = blog.publies(null, PageRequest.of(0, ARTICLES_PAR_PAGE));
        StringBuilder corps = new StringBuilder("<section><h1>").append(e(t.get("blog"))).append("</h1><ul>");
        for (ArticleVue a : page.getContent()) {
            corps.append("<li><a href=\"/blog/").append(a.id()).append("\">").append(e(a.titre())).append("</a> — ").append(e(a.categorie()))
                    .append(", ").append(a.publieLe()).append("<br>").append(e(a.extrait())).append("</li>");
        }
        corps.append("</ul></section>");
        String jsonLd = "{\"@context\":\"https://schema.org\",\"@type\":\"Blog\",\"name\":" + j(parametres.site().nom() + " — " + t.get("blog"))
                + ",\"blogPost\":[" + String.join(",", page.getContent().stream().map(a -> "{\"@type\":\"BlogPosting\",\"headline\":" + j(a.titre())
                + ",\"url\":\"/blog/" + a.id() + "\",\"datePublished\":\"" + a.publieLe() + "\"}").toList()) + "]}";
        return new Page_(t.get("blog") + " — " + parametres.site().nom(), t.get("blogDesc"), "/blog", l, jsonLd, corps.toString(), true);
    }

    private Page_ article(String l, Integer id) {
        Map<String, String> t = TEXTES.get(l);
        ArticleVue a;
        try {
            a = blog.publie(id);
        } catch (RessourceIntrouvableException ex) {
            return null;
        }
        String corps = "<article><p><a href=\"/blog\">" + e(t.get("blog")) + "</a> › " + e(a.categorie()) + "</p><h1>" + e(a.titre()) + "</h1><p>"
                + e(t.get("publie")) + " " + a.publieLe() + " · " + e(a.auteur()) + "</p><div>" + paragraphes(a.contenu()) + "</div></article>";
        String jsonLd = "{\"@context\":\"https://schema.org\",\"@type\":\"BlogPosting\",\"headline\":" + j(a.titre()) + ",\"description\":" + j(a.extrait())
                + ",\"datePublished\":\"" + a.publieLe() + "\",\"author\":{\"@type\":\"Person\",\"name\":" + j(a.auteur()) + "},\"publisher\":{\"@type\":\"Organization\",\"name\":"
                + j(parametres.site().nom()) + "},\"articleSection\":" + j(a.categorie()) + ",\"inLanguage\":\"fr\"}";
        return new Page_(a.titre() + " — " + parametres.site().nom(), a.extrait(), "/blog/" + a.id(), l, jsonLd, corps, true);
    }

    private static String cartes(List<BienResume> liste, Map<String, String> t) {
        StringBuilder s = new StringBuilder("<ul>");
        for (BienResume b : liste) {
            s.append("<li><a href=\"/biens/").append(b.id()).append("\">").append(e(b.titre())).append("</a> — ").append(e(t.get(b.typeOffre().name())))
                    .append(", ").append(e(b.categorie())).append(", ").append(e(b.ville())).append(" · ").append(b.superficie().stripTrailingZeros().toPlainString())
                    .append(" m² · ").append(b.nbChambres()).append(' ').append(e(t.get("chambres"))).append(" · PEB ").append(e(b.peb().etiquette()))
                    .append(" · <strong>").append(prix(b.typeOffre(), b.prix(), t, "fr")).append("</strong></li>");
        }
        return s.append("</ul>").toString();
    }

    private static String prix(TypeOffre type, BigDecimal prix, Map<String, String> t, String l) {
        NumberFormat format = NumberFormat.getIntegerInstance(Locale.forLanguageTag(l.equals("en") ? "en-GB" : l + "-BE"));
        String montant = format.format(prix) + " €";
        return type == TypeOffre.location ? montant + " " + t.get("mois") : montant;
    }

    private static String paragraphes(String texte) {
        StringBuilder s = new StringBuilder();
        for (String p : (texte == null ? "" : texte).split("\\n\\s*\\n")) {
            if (!p.isBlank()) {
                s.append("<p>").append(e(p.trim()).replace("\n", "<br>")).append("</p>");
            }
        }
        return s.toString();
    }

    /** Échappement HTML : tout texte de la base passe par là avant d'entrer dans la page. */
    public static String e(String texte) {
        if (texte == null) {
            return "";
        }
        return texte.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    /** Chaîne JSON entre guillemets, pour les données structurées (et « </ » cassé pour ne jamais fermer le script). */
    static String j(String texte) {
        if (texte == null) {
            return "\"\"";
        }
        StringBuilder s = new StringBuilder("\"");
        for (char c : texte.toCharArray()) {
            switch (c) {
                case '"' -> s.append("\\\"");
                case '\\' -> s.append("\\\\");
                case '\n' -> s.append("\\n");
                case '\r' -> s.append("\\r");
                case '\t' -> s.append("\\t");
                case '/' -> s.append("\\/");
                default -> {
                    if (c < 0x20) {
                        s.append(String.format("\\u%04x", (int) c));
                    } else {
                        s.append(c);
                    }
                }
            }
        }
        return s.append('"').toString();
    }

    private static Map<String, String> texte(String... paires) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < paires.length; i += 2) {
            m.put(paires[i], paires[i + 1]);
        }
        return m;
    }
}
