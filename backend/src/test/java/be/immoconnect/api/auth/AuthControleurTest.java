package be.immoconnect.api.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import be.immoconnect.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

/** Inscription (V7), connexion (M9) et identité : testés sur les comptes de test du livrable 14 (mot de passe « password »). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AuthControleurTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private static final String MEMBRE_SEED = "alice.benali@mail.be";

    @Test
    void laConnexionDUnCompteExistantRenvoieUnJetonEtSonRole() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + MEMBRE_SEED + "\",\"motDePasse\":\"password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.jeton").isNotEmpty())
                .andExpect(jsonPath("$.expireDans").value(3600))
                .andExpect(jsonPath("$.utilisateur.role").value("membre"));
    }

    @Test
    void unMauvaisMotDePasseRenvoie401SansRevelerSiLeCompteExiste() throws Exception {
        for (String email : new String[] {MEMBRE_SEED, "inconnu@mail.be"}) {
            mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + email + "\",\"motDePasse\":\"faux\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("Identifiants invalides"));
        }
    }

    @Test
    void leJetonDonneAccesAuProfilEtSonAbsenceEstRefusee() throws Exception {
        MvcResult connexion = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + MEMBRE_SEED + "\",\"motDePasse\":\"password\"}"))
                .andExpect(status().isOk()).andReturn();
        String jeton = json.readTree(connexion.getResponse().getContentAsString()).get("jeton").asString();

        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(MEMBRE_SEED))
                .andExpect(jsonPath("$.motDePasse").doesNotExist());

        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void lInscriptionCreeUnMembreConnecteEtRefuseLesDoublons() throws Exception {
        String corps = "{\"nom\":\"Dupont\",\"prenom\":\"Léa\",\"email\":\"lea.dupont@test.be\","
                + "\"motDePasse\":\"motdepasse123\",\"langue\":\"nl\"}";
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.jeton").isNotEmpty())
                .andExpect(jsonPath("$.utilisateur.role").value("membre"))
                .andExpect(jsonPath("$.utilisateur.langue").value("nl"));

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isConflict());
    }

    @Test
    void uneInscriptionInvalideDetailleLesChampsEnErreur() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"\",\"prenom\":\"X\",\"email\":\"pas-un-email\",\"motDePasse\":\"court\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.champs.email").exists())
                .andExpect(jsonPath("$.champs.motDePasse").exists())
                .andExpect(jsonPath("$.champs.nom").exists());
    }
}
