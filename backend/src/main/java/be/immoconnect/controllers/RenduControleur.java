package be.immoconnect.controllers;

import be.immoconnect.services.ServiceRenduPublic;
import be.immoconnect.services.ServiceRenduPublic.Page_;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rendu côté serveur des pages publiques (chapitre 10) et plan du site. Le serveur web dirige ici les
 * adresses publiques (/, /a-vendre, /a-louer, /biens, /biens/{id}, /blog, /blog/{id}) : la « coquille »
 * de l'application React (index.html construit, avec ses scripts) est complétée du titre, de la
 * description, des balises hreflang et canonique, des données Schema.org et du contenu visible. Le
 * navigateur affiche cette page tout de suite, puis l'application prend le relais.
 */
@RestController
@Hidden
public class RenduControleur {

    private static final Logger journal = LoggerFactory.getLogger(RenduControleur.class);
    private static final String PREFIXE = "/rendu";
    private static final String RACINE_VIDE = "<div id=\"root\"></div>";
    private static final Duration VALIDITE_COQUILLE = Duration.ofSeconds(60);
    private static final Duration DELAI = Duration.ofSeconds(3);

    private final ServiceRenduPublic service;
    private final String urlCoquille;
    private final String urlSite;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(DELAI).build();
    private volatile String coquille;
    private volatile Instant coquilleLueLe = Instant.EPOCH;

    public RenduControleur(ServiceRenduPublic service,
                           @Value("${immoconnect.rendu.coquille:}") String urlCoquille,
                           @Value("${immoconnect.courriel.url-site}") String urlSite) {
        this.service = service;
        this.urlCoquille = urlCoquille;
        this.urlSite = urlSite.replaceAll("/+$", "");
    }

    @GetMapping(value = {PREFIXE, PREFIXE + "/**"}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> page(HttpServletRequest http, @RequestParam(required = false) String lng) {
        String html = coquille();
        if (html == null) {
            // Sans coquille, pas de page : le serveur web sert alors l'application seule
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        String chemin = http.getRequestURI().substring(http.getContextPath().length() + PREFIXE.length());
        List<String> langues = service.languesActives();
        String langue = langue(lng, http.getHeader(HttpHeaders.ACCEPT_LANGUAGE), langues);
        Page_ page = service.rendre(chemin, langue);
        HttpHeaders entetes = new HttpHeaders();
        entetes.setContentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8));
        entetes.setCacheControl("no-cache");
        entetes.set(HttpHeaders.CONTENT_LANGUAGE, langue);
        entetes.set(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE);
        if (page == null) {
            // Adresse inconnue : l'application affichera sa page « introuvable », le robot reçoit un 404 franc
            return new ResponseEntity<>(html, entetes, HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(injecter(html, page, langues), entetes, HttpStatus.OK);
    }

    /** Plan du site généré depuis les données : pages publiques, biens disponibles, articles publiés, en trois langues. */
    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> planDuSite() {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\" xmlns:xhtml=\"http://www.w3.org/1999/xhtml\">\n");
        List<String> langues = service.languesActives();
        for (String chemin : service.cheminsDuPlan()) {
            xml.append("  <url><loc>").append(urlSite).append(chemin).append("</loc>");
            for (String l : langues) {
                xml.append("<xhtml:link rel=\"alternate\" hreflang=\"").append(l).append("\" href=\"")
                        .append(urlSite).append(chemin).append("?lng=").append(l).append("\"/>");
            }
            xml.append("</url>\n");
        }
        xml.append("</urlset>\n");
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofHours(1))).body(xml.toString());
    }

    /** Consignes aux robots : les pages publiques sont indexables, les espaces connectés et l'API ne le sont pas. */
    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> robots() {
        String texte = String.join("\n", "User-agent: *", "Allow: /",
                "Disallow: /api/", "Disallow: /admin", "Disallow: /profil", "Disallow: /rendez-vous", "Disallow: /favoris",
                "Disallow: /messages", "Disallow: /annonces", "Disallow: /connexion", "Disallow: /inscription",
                "Disallow: /activation", "Disallow: /reinitialisation", "Disallow: /mot-de-passe-oublie", "Disallow: /courriels/",
                "", "Sitemap: " + urlSite + "/sitemap.xml", "");
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofHours(1))).body(texte);
    }

    private String injecter(String html, Page_ page, List<String> langues) {
        String url = ServiceRenduPublic.e(urlSite + page.chemin());
        StringBuilder tete = new StringBuilder();
        tete.append("<meta name=\"description\" content=\"").append(ServiceRenduPublic.e(page.description())).append("\">\n");
        tete.append("<link rel=\"canonical\" href=\"").append(url).append("\">\n");
        if (!page.indexable()) {
            tete.append("<meta name=\"robots\" content=\"noindex\">\n");
        }
        for (String l : langues) {
            tete.append("<link rel=\"alternate\" hreflang=\"").append(l).append("\" href=\"").append(url).append("?lng=").append(l).append("\">\n");
        }
        tete.append("<link rel=\"alternate\" hreflang=\"x-default\" href=\"").append(url).append("\">\n");
        tete.append("<meta property=\"og:title\" content=\"").append(ServiceRenduPublic.e(page.titre())).append("\">\n");
        tete.append("<meta property=\"og:description\" content=\"").append(ServiceRenduPublic.e(page.description())).append("\">\n");
        tete.append("<meta property=\"og:url\" content=\"").append(url).append("\">\n");
        tete.append("<meta property=\"og:type\" content=\"website\">\n");
        tete.append("<script type=\"application/ld+json\">").append(page.jsonLd()).append("</script>\n");
        // Remplacements par position, sans expression régulière : le contenu peut contenir « $ » ou « \ »
        String resultat = remplacerEntre(html, "<title>", "</title>", ServiceRenduPublic.e(page.titre()));
        resultat = resultat.replace("<html lang=\"fr\"", "<html lang=\"" + page.langue() + "\"");
        resultat = inserer(resultat, "</head>", tete.toString());
        return resultat.replace(RACINE_VIDE, "<div id=\"root\">" + page.corps() + "</div>");
    }

    private static String remplacerEntre(String texte, String debut, String fin, String contenu) {
        int a = texte.indexOf(debut);
        int b = a < 0 ? -1 : texte.indexOf(fin, a);
        return a < 0 || b < 0 ? texte : texte.substring(0, a + debut.length()) + contenu + texte.substring(b);
    }

    private static String inserer(String texte, String avant, String ajout) {
        int i = texte.indexOf(avant);
        return i < 0 ? texte : texte.substring(0, i) + ajout + texte.substring(i);
    }

    /** La coquille est relue au plus une fois par minute : une nouvelle version du site est prise en compte sans redémarrage. */
    private String coquille() {
        if (urlCoquille.isBlank()) {
            return null;
        }
        if (coquille != null && Instant.now().isBefore(coquilleLueLe.plus(VALIDITE_COQUILLE))) {
            return coquille;
        }
        try {
            HttpResponse<String> reponse = client.send(HttpRequest.newBuilder(URI.create(urlCoquille)).timeout(DELAI).GET().build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (reponse.statusCode() == 200 && reponse.body().contains(RACINE_VIDE)) {
                coquille = reponse.body();
                coquilleLueLe = Instant.now();
            } else {
                journal.warn("Coquille de l'application inutilisable ({}) : statut {}", urlCoquille, reponse.statusCode());
            }
        } catch (IOException e) {
            journal.warn("Coquille de l'application injoignable ({}) : {}", urlCoquille, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return coquille;
    }

    /** Langue de la page : le paramètre ?lng= des liens hreflang, sinon la préférence du navigateur, sinon le français. */
    private static String langue(String lng, String acceptLanguage, List<String> langues) {
        if (lng != null && langues.contains(lng.toLowerCase(Locale.ROOT))) {
            return lng.toLowerCase(Locale.ROOT);
        }
        if (acceptLanguage != null && !acceptLanguage.isBlank()) {
            try {
                for (Locale.LanguageRange preference : Locale.LanguageRange.parse(acceptLanguage)) {
                    String code = preference.getRange().length() >= 2 ? preference.getRange().substring(0, 2) : "";
                    if (langues.contains(code)) {
                        return code;
                    }
                }
            } catch (IllegalArgumentException e) {
                // En-tête mal formé : on s'en tient au français
            }
        }
        return "fr";
    }
}
