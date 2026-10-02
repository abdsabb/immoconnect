package be.immoconnect.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import be.immoconnect.TestcontainersConfiguration;
import be.immoconnect.services.ServiceRenduPublic;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Chapitre 10 du rapport : rendu côté serveur des pages publiques, balises hreflang et canonique, données
 * Schema.org, plan du site et robots.txt. La « coquille » de l'application (index.html) est servie au test
 * par un petit serveur HTTP local, comme le fait le conteneur du front-end en production.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "immoconnect.courriel.url-site=https://www.immoconnect.test")
@AutoConfigureMockMvc
class RenduPublicTest {

    private static final String COQUILLE = """
            <!doctype html>
            <html lang="fr">
              <head>
                <meta charset="UTF-8" />
                <title>ImmoConnect</title>
              </head>
              <body>
                <div id="root"></div>
                <script type="module" src="/assets/index-abc123.js"></script>
              </body>
            </html>
            """;

    private static HttpServer frontend;

    @DynamicPropertySource
    static void coquille(DynamicPropertyRegistry proprietes) throws IOException {
        frontend = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        frontend.createContext("/index.html", echange -> {
            byte[] corps = COQUILLE.getBytes(StandardCharsets.UTF_8);
            echange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            echange.sendResponseHeaders(200, corps.length);
            echange.getResponseBody().write(corps);
            echange.close();
        });
        frontend.start();
        proprietes.add("immoconnect.rendu.coquille", () -> "http://127.0.0.1:" + frontend.getAddress().getPort() + "/index.html");
    }

    @AfterAll
    static void arreter() {
        frontend.stop(0);
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void lAccueilEstRenduAvecSesBalisesEtLAgence() throws Exception {
        mvc.perform(get("/rendu/"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "fr"))
                .andExpect(content().string(Matchers.containsString("<title>ImmoConnect — Trouvez votre prochain chez-vous à Bruxelles</title>")))
                .andExpect(content().string(Matchers.containsString("<meta name=\"description\" content=\"")))
                .andExpect(content().string(Matchers.containsString("<link rel=\"canonical\" href=\"https://www.immoconnect.test/\">")))
                .andExpect(content().string(Matchers.containsString("hreflang=\"nl\" href=\"https://www.immoconnect.test/?lng=nl\"")))
                .andExpect(content().string(Matchers.containsString("hreflang=\"x-default\"")))
                .andExpect(content().string(Matchers.containsString("\"@type\":\"RealEstateAgent\"")))
                // Le contenu visible est dans la page, et les scripts de l'application sont conservés
                .andExpect(content().string(Matchers.containsString("<div id=\"root\"><section><h1>Trouvez votre prochain chez-vous à Bruxelles</h1>")))
                .andExpect(content().string(Matchers.containsString("/assets/index-abc123.js")))
                .andExpect(content().string(Matchers.containsString("<a href=\"/biens/")));
    }

    @Test
    void uneFicheDeBienPorteSesDonneesStructureesDansLaLangueDemandee() throws Exception {
        Integer bien = jdbc.queryForObject("SELECT MIN(id) FROM bien WHERE statut = 'disponible' AND type_offre = 'vente'", Integer.class);
        String titre = jdbc.queryForObject("SELECT titre FROM bien WHERE id = ?", String.class, bien);
        Integer vues = jdbc.queryForObject("SELECT nb_vues FROM bien WHERE id = ?", Integer.class, bien);

        mvc.perform(get("/rendu/biens/" + bien))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("<h1>" + ServiceRenduPublic.e(titre) + "</h1>")))
                .andExpect(content().string(Matchers.containsString("\"@type\":\"RealEstateListing\"")))
                .andExpect(content().string(Matchers.containsString("\"priceCurrency\":\"EUR\"")))
                .andExpect(content().string(Matchers.containsString("PEB ")))
                // Aperçu lors d'un partage sur un réseau social : la photo de couverture, en adresse absolue
                .andExpect(content().string(Matchers.containsString("<meta property=\"og:image\" content=\"https://www.immoconnect.test/storage/")))
                .andExpect(content().string(Matchers.containsString("<meta property=\"og:url\" content=\"https://www.immoconnect.test/biens/" + bien + "\">")))
                .andExpect(content().string(Matchers.containsString("À vendre")));
        mvc.perform(get("/rendu/biens/" + bien).param("lng", "nl"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "nl"))
                .andExpect(content().string(Matchers.containsString("<html lang=\"nl\"")))
                .andExpect(content().string(Matchers.containsString("Te koop")))
                .andExpect(content().string(Matchers.containsString("slaapkamers")));
        mvc.perform(get("/rendu/biens/" + bien).header("Accept-Language", "en-GB,en;q=0.9,fr;q=0.5"))
                .andExpect(header().string("Content-Language", "en"))
                .andExpect(content().string(Matchers.containsString("For sale")));
        // Le rendu pour les moteurs ne compte pas de vue : seule la consultation par l'application en compte une
        assertThat(jdbc.queryForObject("SELECT nb_vues FROM bien WHERE id = ?", Integer.class, bien)).isEqualTo(vues);
    }

    @Test
    void lesListesEtLeBlogSontRendus() throws Exception {
        mvc.perform(get("/rendu/a-louer"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("<h1>À louer (")))
                .andExpect(content().string(Matchers.containsString("\"@type\":\"ItemList\"")))
                .andExpect(content().string(Matchers.containsString("/ mois")));
        mvc.perform(get("/rendu/blog"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("\"@type\":\"Blog\"")));
        mvc.perform(get("/rendu/contact"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("<h1>Contacter l&#39;agence</h1>")))
                .andExpect(content().string(Matchers.containsString("\"@type\":\"ContactPage\"")))
                // Les coordonnées de l'agence ne figurent plus sur cette page : elle porte un formulaire
                .andExpect(content().string(Matchers.not(Matchers.containsString("contact@immoconnect.be"))))
                .andExpect(content().string(Matchers.containsString("<link rel=\"canonical\" href=\"https://www.immoconnect.test/contact\">")));
        mvc.perform(get("/rendu/blog/3"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("\"@type\":\"BlogPosting\"")))
                .andExpect(content().string(Matchers.containsString("<link rel=\"canonical\" href=\"https://www.immoconnect.test/blog/3\">")));
    }

    @Test
    void uneAdresseInconnueOuNonPubliqueRenvoie404AvecLApplication() throws Exception {
        // Bien inexistant, bien archivé (RA5), article non publié (RA4), espace connecté : jamais de contenu rendu
        Integer archive = jdbc.queryForObject("SELECT MIN(id) FROM bien WHERE statut = 'archive'", Integer.class);
        for (String chemin : new String[] {"/rendu/biens/999999", "/rendu/biens/" + archive, "/rendu/blog/2", "/rendu/profil", "/rendu/admin"}) {
            mvc.perform(get(chemin))
                    .andExpect(status().isNotFound())
                    .andExpect(content().string(Matchers.containsString("<div id=\"root\"></div>")))
                    .andExpect(content().string(Matchers.containsString("/assets/index-abc123.js")));
        }
    }

    @Test
    void lePlanDuSiteEtLesConsignesAuxRobotsViennentDesDonnees() throws Exception {
        Integer bien = jdbc.queryForObject("SELECT MIN(id) FROM bien WHERE statut = 'disponible'", Integer.class);
        Integer archive = jdbc.queryForObject("SELECT MIN(id) FROM bien WHERE statut = 'archive'", Integer.class);
        mvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("<loc>https://www.immoconnect.test/</loc>")))
                .andExpect(content().string(Matchers.containsString("<loc>https://www.immoconnect.test/biens/" + bien + "</loc>")))
                .andExpect(content().string(Matchers.containsString("hreflang=\"en\" href=\"https://www.immoconnect.test/biens/" + bien + "?lng=en\"")))
                .andExpect(content().string(Matchers.containsString("<loc>https://www.immoconnect.test/blog/3</loc>")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("/biens/" + archive + "<"))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("/blog/2<"))));
        mvc.perform(get("/robots.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Disallow: /admin")))
                .andExpect(content().string(Matchers.containsString("Sitemap: https://www.immoconnect.test/sitemap.xml")));
    }

    @Test
    void leTexteDeLaBaseEstEchappe() {
        assertThat(ServiceRenduPublic.e("<script>alert('x')</script> & \"co\""))
                .isEqualTo("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt; &amp; &quot;co&quot;");
    }
}
