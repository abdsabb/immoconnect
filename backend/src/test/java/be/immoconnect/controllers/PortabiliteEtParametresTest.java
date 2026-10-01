package be.immoconnect.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import be.immoconnect.TestcontainersConfiguration;
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

/**
 * Chapitre 11 du rapport : export des données (portabilité) ; cas A5 : paramètres du site.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PortabiliteEtParametresTest {

    private static final String MEMBRE = "alice.benali@mail.be";
    private static final String GESTIONNAIRE = "lotte.goossens@mail.be";
    private static final String EDITEUR = "yasmine.benali@mail.be";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void unMembreTelechargeToutesSesDonnees() throws Exception {
        String membre = connecter(MEMBRE);
        JsonNode export = corps(mvc.perform(get("/api/v1/auth/me/export").header("Authorization", "Bearer " + membre))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", Matchers.containsString("immoconnect-mes-donnees.json")))
                .andExpect(jsonPath("$.compte.email").value(MEMBRE))
                .andExpect(jsonPath("$.compte.role").value("membre"))
                .andExpect(jsonPath("$.compte.cguAccepteesLe").isNotEmpty())
                .andExpect(jsonPath("$.exporteLe").isNotEmpty()));
        // Aucun identifiant interne : les données sont exprimées avec les titres des biens et les noms des interlocuteurs
        assertThat(export.get("favoris").size()).isEqualTo(jdbc.queryForObject(
                "SELECT COUNT(*) FROM favori f JOIN utilisateur u ON u.id = f.membre_id WHERE u.email = ?", Integer.class, MEMBRE));
        assertThat(export.get("visites").size()).isEqualTo(jdbc.queryForObject(
                "SELECT COUNT(*) FROM rendez_vous r JOIN utilisateur u ON u.id = r.membre_id WHERE u.email = ?", Integer.class, MEMBRE));
        assertThat(export.get("historique").valueStream().map(h -> h.get("action").asString())).contains("export_donnees");
        assertThat(export.get("compte").has("id")).isFalse();
        mvc.perform(get("/api/v1/auth/me/export")).andExpect(status().isUnauthorized());
    }

    @Test
    void lesParametresDuSiteSeReglentEtSePublient() throws Exception {
        mvc.perform(get("/api/v1/configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.site.nom").value("ImmoConnect"))
                .andExpect(jsonPath("$.site.languesActives", Matchers.contains("fr", "nl", "en")));
        mvc.perform(get("/api/v1/admin/parametres").header("Authorization", "Bearer " + connecter(EDITEUR)))
                .andExpect(status().isForbidden());

        String gestionnaire = connecter(GESTIONNAIRE);
        mvc.perform(get("/api/v1/admin/parametres").header("Authorization", "Bearer " + gestionnaire))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(7)))
                .andExpect(jsonPath("$[0].cle").value("agence.nom"));

        mvc.perform(put("/api/v1/admin/parametres").header("Authorization", "Bearer " + gestionnaire)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valeurs\":{\"agence.nom\":\"ImmoConnect Bruxelles\",\"langues.actives\":\"fr, nl\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.cle == 'agence.nom')].valeur").value("ImmoConnect Bruxelles"))
                .andExpect(jsonPath("$[?(@.cle == 'agence.nom')].modifiePar").value("Lotte Goossens"));
        mvc.perform(get("/api/v1/configuration"))
                .andExpect(jsonPath("$.site.nom").value("ImmoConnect Bruxelles"))
                .andExpect(jsonPath("$.site.languesActives", Matchers.contains("fr", "nl")));

        // Clé inconnue, langue inconnue, français désactivé, e-mail invalide : refusés champ par champ
        mvc.perform(put("/api/v1/admin/parametres").header("Authorization", "Bearer " + gestionnaire)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"valeurs\":{\"stripe.cle\":\"sk_live\"}}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.champs['stripe.cle']").value("paramètre inconnu"));
        mvc.perform(put("/api/v1/admin/parametres").header("Authorization", "Bearer " + gestionnaire)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"valeurs\":{\"langues.actives\":\"nl,de\"}}"))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(put("/api/v1/admin/parametres").header("Authorization", "Bearer " + gestionnaire)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"valeurs\":{\"langues.actives\":\"nl\"}}"))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(put("/api/v1/admin/parametres").header("Authorization", "Bearer " + gestionnaire)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"valeurs\":{\"agence.email\":\"pas-une-adresse\"}}"))
                .andExpect(status().isUnprocessableContent());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_audit WHERE action = 'modification_parametre'", Integer.class))
                .isEqualTo(2);

        // Remise en l'état pour les autres tests
        mvc.perform(put("/api/v1/admin/parametres").header("Authorization", "Bearer " + gestionnaire)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valeurs\":{\"agence.nom\":\"ImmoConnect\",\"langues.actives\":\"fr,nl,en\"}}"))
                .andExpect(status().isOk());
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
