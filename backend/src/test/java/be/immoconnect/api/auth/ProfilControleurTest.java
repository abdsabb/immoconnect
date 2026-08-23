package be.immoconnect.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import be.immoconnect.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/**
 * Profil (M6) et désinscription (M7, règle RA11) sur un membre du jeu de test qui a déjà
 * pris des rendez-vous et payé des créneaux : le cas exigé pour la démonstration à la défense.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ProfilControleurTest {

    /** charlotte.diallo@mail.be : 7 rendez-vous, 2 paiements, 2 favoris, 5 messages dans les données de test. */
    private static final int MEMBRE_ID = 66;
    private static final String MEMBRE_EMAIL = "charlotte.diallo@mail.be";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    private String connecter(String email, String motDePasse) throws Exception {
        var reponse = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"motDePasse\":\"" + motDePasse + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(reponse.getResponse().getContentAsString()).get("jeton").asString();
    }

    @Test
    void leMembreModifieSonProfilEtSonMotDePasse() throws Exception {
        String jeton = connecter("juliette.nguyen@mail.be", "password");

        mvc.perform(patch("/api/v1/auth/me").header("Authorization", "Bearer " + jeton)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Nguyen-Dupont\",\"prenom\":\"Juliette\",\"telephone\":\"+32 470 11 22 33\",\"langue\":\"en\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Nguyen-Dupont"))
                .andExpect(jsonPath("$.langue").value("en"));

        mvc.perform(put("/api/v1/auth/me/mot-de-passe").header("Authorization", "Bearer " + jeton)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ancienMotDePasse\":\"faux\",\"nouveauMotDePasse\":\"nouveaumdp123\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(put("/api/v1/auth/me/mot-de-passe").header("Authorization", "Bearer " + jeton)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ancienMotDePasse\":\"password\",\"nouveauMotDePasse\":\"nouveaumdp123\"}"))
                .andExpect(status().isNoContent());

        connecter("juliette.nguyen@mail.be", "nouveaumdp123");
    }

    @Test
    void laDesinscriptionAnonymiseLeCompteMaisConserveLHistoriqueMetier() throws Exception {
        int rendezVousAvant = compter("rendez_vous", "membre_id");
        int paiementsAvant = compter("paiement", "membre_id");
        assertThat(rendezVousAvant).isGreaterThan(0);
        assertThat(paiementsAvant).isGreaterThan(0);

        String jeton = connecter(MEMBRE_EMAIL, "password");
        mvc.perform(delete("/api/v1/auth/me").header("Authorization", "Bearer " + jeton))
                .andExpect(status().isNoContent());

        // L'identité a disparu, la connexion est impossible…
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + MEMBRE_EMAIL + "\",\"motDePasse\":\"password\"}"))
                .andExpect(status().isUnauthorized());
        String email = jdbc.queryForObject("SELECT email FROM utilisateur WHERE id = ?", String.class, MEMBRE_ID);
        assertThat(email).isEqualTo("supprime-" + MEMBRE_ID + "@anonyme.immoconnect.be");
        assertThat(jdbc.queryForObject("SELECT telephone FROM membre WHERE utilisateur_id = ?", String.class, MEMBRE_ID)).isNull();

        // … les favoris sont supprimés et les messages vidés …
        assertThat(compter("favori", "membre_id")).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM message WHERE membre_id = ? AND contenu NOT LIKE '[Contenu supprimé%'",
                Integer.class, MEMBRE_ID)).isZero();

        // … mais l'historique métier et la trace d'audit sont conservés (soft delete).
        assertThat(compter("rendez_vous", "membre_id")).isEqualTo(rendezVousAvant);
        assertThat(compter("paiement", "membre_id")).isEqualTo(paiementsAvant);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_audit WHERE utilisateur_id = ? AND action = 'desinscription'",
                Integer.class, MEMBRE_ID)).isEqualTo(1);
    }

    private int compter(String table, String colonne) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + colonne + " = ?", Integer.class, MEMBRE_ID);
    }
}
