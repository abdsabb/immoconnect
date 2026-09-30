package be.immoconnect.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import be.immoconnect.TestcontainersConfiguration;
import java.time.Duration;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Détection d'intrusion (chapitre 9.6 du rapport) : les seuils déclenchent une alerte enregistrée et envoyée,
 * et bannissent l'adresse de la connexion. Les seuils sont abaissés pour le test.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"immoconnect.securite.intrusion.echecs=4", "immoconnect.securite.intrusion.comptes=3"})
class DetectionIntrusionTest {

    private static final String GESTIONNAIRE = "lotte.goossens@mail.be";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private JavaMailSender messagerie;

    @Test
    void uneEnumerationDIdentifiantsBannitLAdresseEtAlerteLesAdministrateurs() throws Exception {
        String attaquant = "203.0.113.9";
        for (String email : new String[] {"a@essai.be", "b@essai.be", "alice.benali@mail.be"}) {
            connexion(email, "mauvais", attaquant).andExpect(status().isUnauthorized());
        }
        // L'adresse est bannie : même les bons identifiants sont refusés, avec le délai à respecter
        connexion("alice.benali@mail.be", "password", attaquant)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
        // Les autres visiteurs ne sont pas touchés
        connexion("alice.benali@mail.be", "password", "198.51.100.7").andExpect(status().isOk());

        attendreAlerte("enumeration", attaquant);
        assertThat(jdbc.queryForObject("SELECT detail FROM alerte_securite WHERE type = 'enumeration' AND ip = ?", String.class, attaquant))
                .startsWith("3 comptes différents");
        // Le super-administrateur est prévenu par e-mail ; une seule alerte par heure pour la même adresse
        verify(messagerie, atLeastOnce()).send(any(SimpleMailMessage.class));
        connexion("c@essai.be", "mauvais", attaquant).andExpect(status().isTooManyRequests());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM alerte_securite WHERE ip = ?", Integer.class, attaquant)).isEqualTo(1);
    }

    @Test
    void uneRafaleDEchecsSurUnCompteDeclencheUneAlerte() throws Exception {
        String attaquant = "203.0.113.20";
        for (int i = 0; i < 4; i++) {
            connexion("lea.vandamme@mail.be", "essai-" + i, attaquant).andExpect(status().isUnauthorized());
        }
        connexion("lea.vandamme@mail.be", "password", attaquant).andExpect(status().isTooManyRequests());
        attendreAlerte("rafale_echecs", attaquant);
    }

    @Test
    void uneCleRevoqueePresenteeEstSignaleeEtLesAlertesSeLisentAuBackOffice() throws Exception {
        String admin = jeton(GESTIONNAIRE);
        JsonNode cle = corps(mvc.perform(post("/api/v1/admin/cles-api").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"libelle\":\"Ancien partenaire\"}"))
                .andExpect(status().isCreated()));
        mvc.perform(patch("/api/v1/admin/cles-api/" + cle.get("id").asInt() + "/revoquer").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/open-data/biens").header("X-API-Key", cle.get("cle").asString()).header("X-Forwarded-For", "203.0.113.30"))
                .andExpect(status().isUnauthorized());
        // Une clé inconnue, elle, ne déclenche rien : ce n'est pas une clé qui a existé
        mvc.perform(get("/api/v1/open-data/biens").header("X-API-Key", "ic_inconnue").header("X-Forwarded-For", "203.0.113.31"))
                .andExpect(status().isUnauthorized());
        attendreAlerte("cle_revoquee", "203.0.113.30");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM alerte_securite WHERE ip = '203.0.113.31'", Integer.class)).isZero();

        mvc.perform(get("/api/v1/admin/alertes").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu[?(@.ip == '203.0.113.30')].type", Matchers.contains("cle_revoquee")))
                .andExpect(jsonPath("$.contenu[?(@.ip == '203.0.113.30')].detail", Matchers.contains("clé « Ancien partenaire »")));
        mvc.perform(get("/api/v1/admin/alertes").header("Authorization", "Bearer " + jeton("yasmine.benali@mail.be")))
                .andExpect(status().isForbidden());
    }

    /** L'alerte est enregistrée hors du fil de la requête : on lui laisse quelques secondes. */
    private void attendreAlerte(String type, String ip) throws InterruptedException {
        long fin = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (System.nanoTime() < fin) {
            if (jdbc.queryForObject("SELECT COUNT(*) FROM alerte_securite WHERE type = ? AND ip = ?", Integer.class, type, ip) > 0) {
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("aucune alerte « " + type + " » pour " + ip);
    }

    private ResultActions connexion(String email, String motDePasse, String ip) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").header("X-Forwarded-For", ip).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"motDePasse\":\"" + motDePasse + "\"}"));
    }

    private String jeton(String email) throws Exception {
        return corps(connexion(email, "password", "192.0.2.10").andExpect(status().isOk())).get("jeton").asString();
    }

    private JsonNode corps(ResultActions reponse) throws Exception {
        return json.readTree(reponse.andReturn().getResponse().getContentAsString());
    }
}
