package be.immoconnect.api.bien;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import be.immoconnect.TestcontainersConfiguration;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Test d'intégration du catalogue public sur un vrai MySQL 8.4 (Testcontainers) peuplé par les
 * migrations Flyway : les données de test du livrable 14 servent de jeu d'essai.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class BienControleurTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void laRechercheRenvoieUnePageDeBiensDisponibles() throws Exception {
        mvc.perform(get("/api/v1/biens").param("taille", "5").param("tri", "prix,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu", Matchers.hasSize(5)))
                .andExpect(jsonPath("$.totalElements", Matchers.greaterThan(5)))
                .andExpect(jsonPath("$.contenu[0].statut").value("disponible"))
                .andExpect(jsonPath("$.contenu[0].agent.nomComplet").isNotEmpty());
    }

    @Test
    void leFiltreParVilleNeRenvoieQueCetteVille() throws Exception {
        mvc.perform(get("/api/v1/biens").param("ville", "Ixelles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu[*].ville", Matchers.everyItem(Matchers.is("Ixelles"))));
    }

    @Test
    void leDetailExposePhotosEtAgentMaisPasLAdresseExacte() throws Exception {
        mvc.perform(get("/api/v1/biens/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.photos", Matchers.not(Matchers.empty())))
                .andExpect(jsonPath("$.agent.telephonePro").isNotEmpty())
                .andExpect(jsonPath("$.adresse").doesNotExist());
    }

    @Test
    void unBienInexistantRenvoieUnProblemDetail404() throws Exception {
        mvc.perform(get("/api/v1/biens/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.type").value("https://www.immoconnect.be/erreurs/introuvable"));
    }

    @Test
    void unTriInconnuEstRefuseAvecUn400() throws Exception {
        mvc.perform(get("/api/v1/biens").param("tri", "motDePasse,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requête invalide"));
    }
}
