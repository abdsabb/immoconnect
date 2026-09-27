package be.immoconnect.api.opendata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
 * Clés API (A7) et Open Data : clé montrée une seule fois et stockée hachée, révocation immédiate
 * (RA12), quota par minute, données réellement anonymisées.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class OpenDataControleurTest {

    private static final String ADMIN = "lotte.goossens@mail.be";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void uneCleNEstMontreeQuUneFoisEtLaBaseNEnGardeQueLEmpreinte() throws Exception {
        String admin = connecter(ADMIN);
        JsonNode creee = generer(admin, "Observatoire du logement");
        String cle = creee.get("cle").asString();
        int id = creee.get("id").asInt();

        assertThat(cle).matches("ic_[0-9a-f]{64}");
        String enBase = jdbc.queryForObject("SELECT cle FROM cle_api WHERE id = ?", String.class, id);
        assertThat(enBase).hasSize(64).doesNotContain(cle.substring(3, 20));
        mvc.perform(get("/api/v1/admin/cles-api").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")].libelle", Matchers.contains("Observatoire du logement")))
                .andExpect(jsonPath("$[?(@.id == " + id + ")].creeePar", Matchers.contains("Lotte Goossens")))
                .andExpect(jsonPath("$[*].cle", Matchers.everyItem(Matchers.nullValue())));
    }

    @Test
    void lAccesExigeUneCleValideEtLaRevocationEstImmediate() throws Exception {
        String admin = connecter(ADMIN);
        JsonNode creee = generer(admin, "Comparateur de prix");
        String cle = creee.get("cle").asString();

        mvc.perform(get("/api/v1/open-data/biens"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://www.immoconnect.be/erreurs/non-authentifie"));
        mvc.perform(get("/api/v1/open-data/biens").header("X-API-Key", "ic_" + "0".repeat(64)))
                .andExpect(status().isUnauthorized());
        // Un jeton d'utilisateur n'ouvre pas l'API ouverte : ce sont deux niveaux d'accès distincts
        mvc.perform(get("/api/v1/open-data/biens").header("Authorization", "Bearer " + admin))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/v1/open-data/biens").header("X-API-Key", cle))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Limit", "60"));
        assertThat(jdbc.queryForObject("SELECT derniere_utilisation IS NOT NULL FROM cle_api WHERE id = ?", Boolean.class,
                creee.get("id").asInt())).isTrue();

        mvc.perform(patch("/api/v1/admin/cles-api/" + creee.get("id").asInt() + "/revoquer").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        mvc.perform(get("/api/v1/open-data/biens").header("X-API-Key", cle))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void lesAnnoncesOuvertesNeContiennentAucuneDonneePersonnelle() throws Exception {
        String cle = generer(connecter(ADMIN), "Étude de marché").get("cle").asString();

        String reponse = mvc.perform(get("/api/v1/open-data/biens").header("X-API-Key", cle))
                .andExpect(status().isOk())
                .andExpect(header().string("Link", Matchers.containsString("creativecommons.org/licenses/by/4.0")))
                .andExpect(jsonPath("$", Matchers.hasSize(Matchers.greaterThan(50))))
                .andExpect(jsonPath("$[0].ville").isNotEmpty())
                .andExpect(jsonPath("$[0].prix").isNumber())
                .andExpect(jsonPath("$[0].adresse").doesNotExist())
                .andExpect(jsonPath("$[0].agent").doesNotExist())
                .andExpect(jsonPath("$[0].id").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        // Aucune adresse ni aucun nom d'agent des données de test n'apparaît dans le jeu de données
        assertThat(reponse).doesNotContain("Avenue de la Toison d'Or", "Dubois", "@mail.be", "+32");
        for (JsonNode bien : json.readTree(reponse)) {
            assertThat(bien.get("latitude").decimalValue().scale()).isLessThanOrEqualTo(3);
        }
    }

    @Test
    void leJeuDeDonneesExisteAussiEnCsvEtEnStatistiques() throws Exception {
        String cle = generer(connecter(ADMIN), "Tableur").get("cle").asString();

        String csv = mvc.perform(get("/api/v1/open-data/biens").header("X-API-Key", cle).accept("text/csv"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andReturn().getResponse().getContentAsString();
        assertThat(csv.lines().findFirst().orElseThrow())
                .isEqualTo("categorie;ville;code_postal;prix;superficie;nb_chambres;latitude;longitude;publie_le");
        assertThat(csv.lines().count()).isGreaterThan(50);

        mvc.perform(get("/api/v1/open-data/statistiques").header("X-API-Key", cle))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].commune").isNotEmpty())
                .andExpect(jsonPath("$[0].categorie").isNotEmpty())
                .andExpect(jsonPath("$[0].nbAnnonces", Matchers.greaterThan(0)))
                .andExpect(jsonPath("$[0].prixMoyen", Matchers.greaterThan(0)))
                .andExpect(jsonPath("$[0].prixMedianM2", Matchers.greaterThan(0)));
    }

    @Test
    void leQuotaParMinuteEstAppliqueCleParCle() throws Exception {
        String admin = connecter(ADMIN);
        String cle = generer(admin, "Client trop pressé").get("cle").asString();
        String autreCle = generer(admin, "Client raisonnable").get("cle").asString();

        int acceptes = 0;
        int refuses = 0;
        for (int appel = 0; appel < 65; appel++) {
            int statut = mvc.perform(get("/api/v1/open-data/statistiques").header("X-API-Key", cle)).andReturn().getResponse().getStatus();
            if (statut == 200) {
                acceptes++;
            } else if (statut == 429) {
                refuses++;
            }
        }
        // Une minute peut basculer pendant le test : le quota est alors rechargé une fois
        assertThat(acceptes).isBetween(60, 65);
        assertThat(acceptes + refuses).isEqualTo(65);
        if (refuses > 0) {
            mvc.perform(get("/api/v1/open-data/statistiques").header("X-API-Key", cle))
                    .andExpect(status().is(Matchers.anyOf(Matchers.is(429), Matchers.is(200))));
        }
        mvc.perform(get("/api/v1/open-data/statistiques").header("X-API-Key", autreCle)).andExpect(status().isOk());
    }

    @Test
    void lesClesApiSontGereesParUnGestionnaire() throws Exception {
        mvc.perform(get("/api/v1/admin/cles-api").header("Authorization", "Bearer " + connecter("yasmine.benali@mail.be")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/cles-api").header("Authorization", "Bearer " + connecter("sarah.dubois@mail.be"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"libelle\":\"Pour moi\"}"))
                .andExpect(status().isForbidden());
    }

    private JsonNode generer(String admin, String libelle) throws Exception {
        return corps(mvc.perform(post("/api/v1/admin/cles-api").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"libelle\":\"" + libelle + "\"}"))
                .andExpect(status().isCreated()));
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
