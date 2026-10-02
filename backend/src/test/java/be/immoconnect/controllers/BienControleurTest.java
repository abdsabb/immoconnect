package be.immoconnect.controllers;

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

    /** Le site montre les biens disponibles, sous option, vendus et loués ; jamais un bien hors ligne (RA5). */
    @Test
    void laRechercheRenvoieLesBiensEnLigneQuelQueSoitLeurStatut() throws Exception {
        mvc.perform(get("/api/v1/biens").param("taille", "100").param("tri", "prix,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu", Matchers.hasSize(100)))
                .andExpect(jsonPath("$.contenu[*].statut", Matchers.hasItems("disponible", "sous_option", "vendu", "loue")))
                .andExpect(jsonPath("$.contenu[*].statut", Matchers.not(Matchers.hasItem("archive"))))
                .andExpect(jsonPath("$.contenu[0].agent.nomComplet").isNotEmpty());
        // Le filtre par statut reste disponible
        mvc.perform(get("/api/v1/biens").param("statut", "disponible").param("taille", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu[*].statut", Matchers.everyItem(Matchers.is("disponible"))));
    }

    /** L'agence travaille à Bruxelles : les annonces de test de quatre villes hors Région ont été relocalisées (V13). */
    @Test
    void aucuneAnnonceNEstSitueeANamurNivellesLiegeOuLouvainLaNeuve() throws Exception {
        for (String ville : new String[] {"Namur", "Nivelles", "Liège", "Louvain-la-Neuve"}) {
            mvc.perform(get("/api/v1/biens").param("ville", ville))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }
        mvc.perform(get("/api/v1/biens").param("ville", "Koekelberg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu[0].codePostal").value("1081"))
                .andExpect(jsonPath("$.contenu[0].titre", Matchers.containsString("Koekelberg")));
    }

    @Test
    void leFiltreParVilleNeRenvoieQueCetteVille() throws Exception {
        mvc.perform(get("/api/v1/biens").param("ville", "Ixelles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu[*].ville", Matchers.everyItem(Matchers.is("Ixelles"))));
    }

    @Test
    void laCommuneSeChercheAussiParSonCodePostal() throws Exception {
        mvc.perform(get("/api/v1/biens").param("ville", "1050"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu", Matchers.not(Matchers.empty())))
                .andExpect(jsonPath("$.contenu[*].codePostal", Matchers.everyItem(Matchers.is("1050"))));
    }

    @Test
    void unBienHorsLigneNeSeListePas() throws Exception {
        mvc.perform(get("/api/v1/biens").param("statut", "archive"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/biens").param("statut", "vendu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu[*].statut", Matchers.everyItem(Matchers.is("vendu"))));
    }

    @Test
    void leFiltreParTypeDOffreSepareLesVentesDesLocations() throws Exception {
        // Biens disponibles seulement : les annonces créées par les autres tests ne faussent pas le contrôle des loyers
        mvc.perform(get("/api/v1/biens").param("typeOffre", "location").param("statut", "disponible").param("taille", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu", Matchers.hasSize(Matchers.greaterThanOrEqualTo(10))))
                .andExpect(jsonPath("$.contenu[*].typeOffre", Matchers.everyItem(Matchers.is("location"))))
                // Le prix d'une location est un loyer mensuel
                .andExpect(jsonPath("$.contenu[*].prix", Matchers.everyItem(Matchers.lessThanOrEqualTo(3500.0))))
                .andExpect(jsonPath("$.contenu[*].titre", Matchers.everyItem(Matchers.not(Matchers.containsString("(location)")))));

        mvc.perform(get("/api/v1/biens").param("typeOffre", "vente").param("taille", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu", Matchers.hasSize(Matchers.greaterThanOrEqualTo(10))))
                .andExpect(jsonPath("$.contenu[*].typeOffre", Matchers.everyItem(Matchers.is("vente"))))
                .andExpect(jsonPath("$.contenu[*].prix", Matchers.everyItem(Matchers.greaterThan(10000.0))));
    }

    @Test
    void unTypeDOffreInconnuEstRefuseAvecUn400() throws Exception {
        mvc.perform(get("/api/v1/biens").param("typeOffre", "echange"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void leDetailExposePhotosEtAgentMaisPasLAdresseExacte() throws Exception {
        mvc.perform(get("/api/v1/biens/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.typeOffre").value("vente"))
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
