package be.immoconnect.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import be.immoconnect.TestcontainersConfiguration;
import be.immoconnect.config.ConfigurationHorloge;
import java.time.LocalDate;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Blog (V5, A2, règle RA4) et traductions du site (A6). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class BlogEtTraductionsTest {

    /** Éditeur : niveau d'accès 1, suffisant pour le blog et les traductions. */
    private static final String EDITEUR = "yasmine.benali@mail.be";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void lePublicNeLitQueLesArticlesPublies() throws Exception {
        mvc.perform(get("/api/v1/articles").param("taille", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu", Matchers.hasSize(50)))
                .andExpect(jsonPath("$.contenu[*].statut", Matchers.everyItem(Matchers.is("publie"))))
                .andExpect(jsonPath("$.contenu[0].extrait").isNotEmpty())
                .andExpect(jsonPath("$.contenu[0].contenu").isEmpty());
        // Les articles 1 (archivé) et 2 (brouillon) des données de test sont introuvables pour le public
        mvc.perform(get("/api/v1/articles/1")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/articles/2")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/articles/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu").isNotEmpty())
                .andExpect(jsonPath("$.categorie").isNotEmpty());
        mvc.perform(get("/api/v1/articles/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(6)));
        // Aucun article n'est publié dans le futur
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM article WHERE publie_le > ?", Integer.class,
                LocalDate.now(ConfigurationHorloge.FUSEAU_AGENCE))).isZero();
    }

    @Test
    void lesArticlesSeFiltrentParCategorieDuPlusRecentAuPlusAncien() throws Exception {
        JsonNode page = corps(mvc.perform(get("/api/v1/articles").param("categorieId", "4").param("taille", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu[*].categorieId", Matchers.everyItem(Matchers.is(4)))));
        String precedente = "9999-12-31";
        for (JsonNode article : page.get("contenu")) {
            assertThat(article.get("publieLe").asString()).isLessThanOrEqualTo(precedente);
            precedente = article.get("publieLe").asString();
        }
    }

    @Test
    void unArticleNaitBrouillonPuisEstPubliePuisArchive() throws Exception {
        String editeur = connecter(EDITEUR);

        int id = corps(mvc.perform(post("/api/v1/admin/articles").header("Authorization", "Bearer " + editeur)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titre": "Visiter en soirée : nos créneaux premium", "categorieId": 6,
                                 "contenu": "Vous travaillez en journée ? Nos agents vous reçoivent désormais jusqu'à 20 h.\\n\\n<script>alert(1)</script>"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("brouillon"))
                .andExpect(jsonPath("$.publieLe").isEmpty())
                .andExpect(jsonPath("$.auteur").value("Yasmine Benali"))).get("id").asInt();
        mvc.perform(get("/api/v1/articles/" + id)).andExpect(status().isNotFound());

        mvc.perform(patch("/api/v1/admin/articles/" + id + "/publier").header("Authorization", "Bearer " + editeur))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("publie"))
                .andExpect(jsonPath("$.publieLe").value(LocalDate.now(ConfigurationHorloge.FUSEAU_AGENCE).toString()));
        // Le contenu est rendu tel quel, en texte : c'est l'affichage qui n'interprète aucune balise
        mvc.perform(get("/api/v1/articles/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu", Matchers.containsString("<script>")));
        mvc.perform(get("/api/v1/articles").param("taille", "1"))
                .andExpect(jsonPath("$.contenu[0].id").value(id));

        mvc.perform(put("/api/v1/admin/articles/" + id).header("Authorization", "Bearer " + editeur)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titre\": \"Visiter en soirée\", \"categorieId\": 6, \"contenu\": \"Texte corrigé.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titre").value("Visiter en soirée"));
        mvc.perform(patch("/api/v1/admin/articles/" + id + "/archiver").header("Authorization", "Bearer " + editeur))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("archive"));
        mvc.perform(get("/api/v1/articles/" + id)).andExpect(status().isNotFound());

        mvc.perform(delete("/api/v1/admin/articles/" + id).header("Authorization", "Bearer " + editeur))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/admin/articles/" + id).header("Authorization", "Bearer " + editeur))
                .andExpect(status().isNotFound());
    }

    @Test
    void laGestionDuBlogEstReserveeAuxAdministrateurs() throws Exception {
        mvc.perform(get("/api/v1/admin/articles")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/admin/articles").header("Authorization", "Bearer " + connecter("sarah.dubois@mail.be"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titre\": \"Intrus\", \"categorieId\": 1, \"contenu\": \"Texte\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/articles").header("Authorization", "Bearer " + connecter(EDITEUR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titre\": \" \", \"categorieId\": 999, \"contenu\": \"\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.champs.titre").isNotEmpty())
                .andExpect(jsonPath("$.champs.contenu").isNotEmpty());
    }

    @Test
    void unTexteDuSiteModifieParLAdministrateurEstServiAussitot() throws Exception {
        String editeur = connecter(EDITEUR);

        mvc.perform(get("/api/v1/admin/traductions").header("Authorization", "Bearer " + editeur))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(Matchers.greaterThanOrEqualTo(50))))
                .andExpect(jsonPath("$[?(@.cle == 'accueil.titre')].valeurs.nl").isNotEmpty());

        // Seul le néerlandais est transmis : le français et l'anglais ne changent pas
        String francais = corps(mvc.perform(get("/api/v1/traductions/fr"))).get("accueil.tout_voir").asString();
        mvc.perform(put("/api/v1/admin/traductions/accueil.tout_voir").header("Authorization", "Bearer " + editeur)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"valeurs\": {\"nl\": \"Alle panden bekijken\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valeurs.nl").value("Alle panden bekijken"))
                .andExpect(jsonPath("$.valeurs.fr").value(francais));
        mvc.perform(get("/api/v1/traductions/nl"))
                .andExpect(jsonPath("$['accueil.tout_voir']").value("Alle panden bekijken"));

        // Nouvelle clé, puis suppression dans toutes les langues
        mvc.perform(put("/api/v1/admin/traductions/accueil.bandeau").header("Authorization", "Bearer " + editeur)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valeurs\": {\"fr\": \"Portes ouvertes ce samedi\", \"nl\": \"Opendeurdag deze zaterdag\", \"en\": \"Open day this Saturday\"}}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/traductions/en")).andExpect(jsonPath("$['accueil.bandeau']").value("Open day this Saturday"));
        mvc.perform(delete("/api/v1/admin/traductions/accueil.bandeau").header("Authorization", "Bearer " + editeur))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/traductions/en")).andExpect(jsonPath("$['accueil.bandeau']").doesNotExist());
    }

    @Test
    void uneTraductionInvalideEstRefusee() throws Exception {
        String editeur = connecter(EDITEUR);

        mvc.perform(put("/api/v1/admin/traductions/Accueil Titre").header("Authorization", "Bearer " + editeur)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"valeurs\": {\"fr\": \"Texte\"}}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.champs.cle").isNotEmpty());
        mvc.perform(put("/api/v1/admin/traductions/accueil.titre").header("Authorization", "Bearer " + editeur)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"valeurs\": {\"de\": \"Text\"}}"))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(put("/api/v1/admin/traductions/accueil.titre").header("Authorization", "Bearer " + connecter("alice.benali@mail.be"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"valeurs\": {\"fr\": \"Piraté\"}}"))
                .andExpect(status().isForbidden());
    }

    private JsonNode corps(ResultActions reponse) throws Exception {
        return json.readTree(reponse.andReturn().getResponse().getContentAsString());
    }

    private String connecter(String email) throws Exception {
        return corps(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"motDePasse\":\"password\"}"))
                .andExpect(status().isOk())).get("jeton").asString();
    }
}
