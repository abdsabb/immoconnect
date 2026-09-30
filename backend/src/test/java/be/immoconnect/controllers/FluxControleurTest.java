package be.immoconnect.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import be.immoconnect.LectureXml;
import be.immoconnect.TestcontainersConfiguration;
import be.immoconnect.services.ServiceFlux;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Flux RSS publics, sur un vrai MySQL 8.4 peuplé par les migrations Flyway. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class FluxControleurTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void leFluxDuBlogEstPublicEtNeMontreQueLesArticlesPublies() throws Exception {
        Document flux = flux("/api/v1/flux/articles");

        List<String> liens = textes(flux, "guid");
        assertThat(liens).hasSize(Math.min(ServiceFlux.ELEMENTS,
                jdbc.queryForObject("select count(*) from article where statut = 'publie'", Integer.class)));
        assertThat(liens).allMatch(lien -> lien.matches(".+/blog/\\d+"));
        // RA4 : ni brouillon ni article archivé — comparés par identifiant, deux articles pouvant porter le même titre
        List<String> masques = jdbc.queryForList("select concat('/blog/', id) from article where statut <> 'publie'", String.class);
        assertThat(masques).isNotEmpty();
        assertThat(liens).noneMatch(lien -> masques.stream().anyMatch(lien::endsWith));
        // Le plus récent en premier
        String dernier = jdbc.queryForObject(
                "select titre from article where statut = 'publie' order by publie_le desc, id desc limit 1", String.class);
        assertThat(textes(flux, "title").get(1)).isEqualTo(dernier);
    }

    @Test
    void leFluxDesAnnoncesSeFiltreParTypeDOffre() throws Exception {
        Document locations = flux("/api/v1/flux/biens?typeOffre=location");
        assertThat(textes(locations, "title").getFirst()).isEqualTo("ImmoConnect — biens à louer");
        assertThat(descriptions(locations)).isNotEmpty().allMatch(d -> d.startsWith("À louer · ") && d.contains(" € par mois"));

        Document ventes = flux("/api/v1/flux/biens?typeOffre=vente");
        assertThat(descriptions(ventes)).hasSize(ServiceFlux.ELEMENTS)
                .allMatch(d -> d.startsWith("À vendre · ") && !d.contains("par mois"));

        assertThat(textes(flux("/api/v1/flux/biens"), "title").getFirst()).isEqualTo("ImmoConnect — biens à vendre et à louer");
        mvc.perform(get("/api/v1/flux/biens").param("typeOffre", "echange")).andExpect(status().isBadRequest());
    }

    @Test
    void unFluxNeMontreQueLesBiensDisponiblesSansAdresseNiAgent() throws Exception {
        String xml = xml("/api/v1/flux/biens");

        List<Integer> identifiants = textes(LectureXml.lire(xml), "guid").stream()
                .map(lien -> Integer.valueOf(lien.substring(lien.lastIndexOf('/') + 1))).toList();
        assertThat(identifiants).isNotEmpty().allSatisfy(id -> assertThat(
                jdbc.queryForObject("select statut from bien where id = ?", String.class, id)).isEqualTo("disponible"));
        for (Integer id : identifiants) {
            assertThat(xml).doesNotContain(jdbc.queryForObject("select adresse from bien where id = ?", String.class, id));
        }
        assertThat(xml).doesNotContain("@mail.be", "+32");
        // Nom complet : un nom seul, « Lambert », se retrouve dans « Woluwe-Saint-Lambert »
        assertThat(jdbc.queryForList("select concat(u.prenom, ' ', u.nom) from utilisateur u "
                + "join agent_immobilier a on a.utilisateur_id = u.id", String.class))
                .isNotEmpty().noneMatch(xml::contains);
    }

    @Test
    void unLecteurDeFluxNAPasBesoinDeJeton() throws Exception {
        mvc.perform(get("/api/v1/flux/articles"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/rss+xml"))
                .andExpect(header().string("Cache-Control", "max-age=900, public"));
        // Un flux se lit, il ne s'écrit pas
        mvc.perform(post("/api/v1/flux/articles")).andExpect(status().isUnauthorized());
    }

    private Document flux(String adresse) throws Exception {
        return LectureXml.lire(xml(adresse));
    }

    private String xml(String adresse) throws Exception {
        return mvc.perform(get(adresse)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static List<String> textes(Document flux, String balise) {
        NodeList noeuds = flux.getElementsByTagName(balise);
        List<String> textes = new ArrayList<>();
        for (int i = 0; i < noeuds.getLength(); i++) {
            textes.add(noeuds.item(i).getTextContent());
        }
        return textes;
    }

    /** Descriptions des éléments, sans celle du canal. */
    private static List<String> descriptions(Document flux) {
        NodeList elements = flux.getElementsByTagName("item");
        List<String> descriptions = new ArrayList<>();
        for (int i = 0; i < elements.getLength(); i++) {
            descriptions.add(((Element) elements.item(i)).getElementsByTagName("description").item(0).getTextContent());
        }
        return descriptions;
    }
}
