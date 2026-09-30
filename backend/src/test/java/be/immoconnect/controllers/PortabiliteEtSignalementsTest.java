package be.immoconnect.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
 * Chapitre 11 du rapport : export des données (portabilité), signalement de contenus (règlement sur
 * les services numériques) et paramètres du site (cas A5).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PortabiliteEtSignalementsTest {

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
    void unSignalementEstTrancheParUnGestionnaireEtLeContenuRetire() throws Exception {
        String membre = connecter(MEMBRE);
        int bien = jdbc.queryForObject("SELECT MIN(id) FROM bien WHERE statut = 'disponible'", Integer.class);
        String signalement = "{\"typeContenu\":\"bien\",\"contenuId\":" + bien + ",\"motif\":\"arnaque\","
                + "\"description\":\"Cette annonce copie mot pour mot une annonce d'un autre site.\"}";

        mvc.perform(post("/api/v1/signalements").contentType(MediaType.APPLICATION_JSON).content(signalement))
                .andExpect(status().isUnauthorized());
        int id = corps(mvc.perform(post("/api/v1/signalements").header("Authorization", "Bearer " + membre)
                        .contentType(MediaType.APPLICATION_JSON).content(signalement))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("ouvert"))
                .andExpect(jsonPath("$.apercu").isNotEmpty())).get("id").asInt();
        // Un seul signalement ouvert par contenu et par personne
        mvc.perform(post("/api/v1/signalements").header("Authorization", "Bearer " + membre)
                        .contentType(MediaType.APPLICATION_JSON).content(signalement))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.champs.contenuId").isNotEmpty());

        // L'éditeur (niveau 1) ne voit pas les signalements, le gestionnaire (niveau 2) oui
        mvc.perform(get("/api/v1/admin/signalements").header("Authorization", "Bearer " + connecter(EDITEUR)))
                .andExpect(status().isForbidden());
        String gestionnaire = connecter(GESTIONNAIRE);
        mvc.perform(get("/api/v1/admin/signalements").param("statut", "ouvert").header("Authorization", "Bearer " + gestionnaire))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu[0].id").value(id))
                .andExpect(jsonPath("$.contenu[0].auteur").value("Alice Benali"));

        mvc.perform(patch("/api/v1/admin/signalements/" + id).header("Authorization", "Bearer " + gestionnaire)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\":\"retire\",\"decision\":\"Annonce dupliquée : retirée en attendant les explications de l'agent.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("retire"))
                .andExpect(jsonPath("$.traitePar").value("Lotte Goossens"))
                .andExpect(jsonPath("$.traiteLe").isNotEmpty());
        // Le bien retiré n'est plus public ; la décision est définitive et journalisée (RA13)
        mvc.perform(get("/api/v1/biens/" + bien)).andExpect(status().isNotFound());
        mvc.perform(patch("/api/v1/admin/signalements/" + id).header("Authorization", "Bearer " + gestionnaire)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"statut\":\"conserve\",\"decision\":\"Finalement non.\"}"))
                .andExpect(status().isUnprocessableContent());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_audit WHERE action = 'retrait_contenu' AND entite = ?",
                Integer.class, "signalement#" + id)).isEqualTo(1);
    }

    @Test
    void onNeSignaleQuUnMessageQueLOnARecu() throws Exception {
        // Un message envoyé par un membre à un agent : le membre ne peut pas le signaler, l'agent si
        Integer message = jdbc.queryForObject("SELECT MIN(id) FROM message WHERE expediteur = 'membre'", Integer.class);
        String membreExpediteur = jdbc.queryForObject("SELECT u.email FROM message m JOIN utilisateur u ON u.id = m.membre_id WHERE m.id = ?",
                String.class, message);
        String agentDestinataire = jdbc.queryForObject("SELECT u.email FROM message m JOIN utilisateur u ON u.id = m.agent_id WHERE m.id = ?",
                String.class, message);
        String corps = "{\"typeContenu\":\"message\",\"contenuId\":" + message + ",\"motif\":\"indesirable\",\"description\":\"Publicité.\"}";
        mvc.perform(post("/api/v1/signalements").header("Authorization", "Bearer " + connecter(membreExpediteur))
                        .contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isForbidden());
        int id = corps(mvc.perform(post("/api/v1/signalements").header("Authorization", "Bearer " + connecter(agentDestinataire))
                        .contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isCreated())).get("id").asInt();
        mvc.perform(patch("/api/v1/admin/signalements/" + id).header("Authorization", "Bearer " + connecter(GESTIONNAIRE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\":\"retire\",\"decision\":\"Message publicitaire sans rapport avec une visite.\"}"))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT contenu FROM message WHERE id = ?", String.class, message))
                .isEqualTo("[Message retiré par la modération]");
    }

    @Test
    void unSignalementInvalideEstRefuse() throws Exception {
        String membre = connecter(MEMBRE);
        mvc.perform(post("/api/v1/signalements").header("Authorization", "Bearer " + membre)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"typeContenu\":\"bien\",\"contenuId\":1,\"motif\":\"autre\",\"description\":\" \"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.champs.description").isNotEmpty());
        mvc.perform(post("/api/v1/signalements").header("Authorization", "Bearer " + membre)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"typeContenu\":\"article\",\"contenuId\":999999,\"motif\":\"autre\",\"description\":\"Test\"}"))
                .andExpect(status().isNotFound());
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
