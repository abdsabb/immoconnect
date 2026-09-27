package be.immoconnect.api.favori;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/** Favoris (M1, M2) : ajout et retrait idempotents, règle RA5, cloisonnement entre membres. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class FavoriControleurTest {

    /** Biens disponibles ; le bien 1 est vendu. */
    private static final int BIEN = 10;
    private static final int AUTRE_BIEN = 5;
    private static final int BIEN_VENDU = 1;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void leMembreAjouteConsultePuisRetireUnFavori() throws Exception {
        String membre = inscrire("favoris.un@test.immoconnect.be");

        mvc.perform(put("/api/v1/biens/" + BIEN + "/favori").header("Authorization", "Bearer " + membre))
                .andExpect(status().isNoContent());
        mvc.perform(put("/api/v1/biens/" + AUTRE_BIEN + "/favori").header("Authorization", "Bearer " + membre))
                .andExpect(status().isNoContent());
        // Idempotence : ajouter une seconde fois ne crée pas de doublon
        mvc.perform(put("/api/v1/biens/" + BIEN + "/favori").header("Authorization", "Bearer " + membre))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/membres/moi/favoris").header("Authorization", "Bearer " + membre))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.contenu[*].id", Matchers.containsInAnyOrder(BIEN, AUTRE_BIEN)))
                .andExpect(jsonPath("$.contenu[0].titre").isNotEmpty())
                .andExpect(jsonPath("$.contenu[0].adresse").doesNotExist());

        mvc.perform(delete("/api/v1/biens/" + BIEN + "/favori").header("Authorization", "Bearer " + membre))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/biens/" + BIEN + "/favori").header("Authorization", "Bearer " + membre))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/membres/moi/favoris").header("Authorization", "Bearer " + membre))
                .andExpect(jsonPath("$.contenu[*].id", Matchers.contains(AUTRE_BIEN)));
    }

    @Test
    void lesFavorisDUnMembreNeSontPasCeuxDUnAutre() throws Exception {
        String premier = inscrire("favoris.deux@test.immoconnect.be");
        String second = inscrire("favoris.trois@test.immoconnect.be");

        mvc.perform(put("/api/v1/biens/" + BIEN + "/favori").header("Authorization", "Bearer " + premier))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/membres/moi/favoris").header("Authorization", "Bearer " + second))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void unBienVenduOuInexistantNeDevientPasFavori() throws Exception {
        String membre = inscrire("favoris.quatre@test.immoconnect.be");

        mvc.perform(put("/api/v1/biens/" + BIEN_VENDU + "/favori").header("Authorization", "Bearer " + membre))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://www.immoconnect.be/erreurs/bien-indisponible"));
        mvc.perform(put("/api/v1/biens/999999/favori").header("Authorization", "Bearer " + membre))
                .andExpect(status().isNotFound());
    }

    @Test
    void lesFavorisSontReservesAuxMembres() throws Exception {
        mvc.perform(put("/api/v1/biens/" + BIEN + "/favori")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/membres/moi/favoris")).andExpect(status().isUnauthorized());

        String agent = json.readTree(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"sarah.dubois@mail.be\",\"motDePasse\":\"password\"}"))
                .andReturn().getResponse().getContentAsString()).get("jeton").asString();
        mvc.perform(put("/api/v1/biens/" + BIEN + "/favori").header("Authorization", "Bearer " + agent))
                .andExpect(status().isForbidden());
    }

    private String inscrire(String email) throws Exception {
        return json.readTree(mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Test\",\"prenom\":\"Favoris\",\"email\":\"" + email + "\",\"motDePasse\":\"motdepasse123\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("jeton").asString();
    }
}
