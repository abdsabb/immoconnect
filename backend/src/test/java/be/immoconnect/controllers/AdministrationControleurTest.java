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
import java.time.LocalDateTime;
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
 * Back-office de l'administrateur : comptes (A1), catégories (A3), journal d'audit (A4),
 * statistiques (A8), et les niveaux d'accès qui les protègent.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AdministrationControleurTest {

    /** Niveaux d'accès 3, 2 et 1 dans les données de test. */
    private static final String SUPER_ADMIN = "david.moreau@mail.be";
    private static final String GESTIONNAIRE = "lotte.goossens@mail.be";
    private static final int GESTIONNAIRE_ID = 110;
    private static final String EDITEUR = "yasmine.benali@mail.be";
    private static final int SUPER_ADMIN_ID = 109;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void lAdministrateurOuvreUnCompteAgentQuiPeutAussitotTravailler() throws Exception {
        String admin = connecter(SUPER_ADMIN, "password");

        JsonNode agent = corps(mvc.perform(post("/api/v1/admin/agents").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nom": "Wouters", "prenom": "Elise", "email": "Elise.Wouters@test.immoconnect.be",
                                 "telephonePro": "+32 470 55 66 77", "langue": "nl", "motDePasse": "provisoire-2026"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("agent"))
                .andExpect(jsonPath("$.email").value("elise.wouters@test.immoconnect.be"))
                .andExpect(jsonPath("$.matricule", Matchers.matchesPattern("AG-\\d{4}-\\d{3}")))
                .andExpect(jsonPath("$.actif").value(true))
                .andExpect(jsonPath("$.motDePasse").doesNotExist()));

        String jetonAgent = connecter("elise.wouters@test.immoconnect.be", "provisoire-2026");
        mvc.perform(get("/api/v1/agents/moi/biens").header("Authorization", "Bearer " + jetonAgent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.empty()));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_audit WHERE action = 'creation_compte_agent' AND entite = ?",
                Integer.class, "utilisateur#" + agent.get("id").asInt())).isEqualTo(1);

        // Une adresse déjà utilisée est refusée
        mvc.perform(post("/api/v1/admin/agents").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nom": "Dubois", "prenom": "Sarah", "email": "sarah.dubois@mail.be",
                                 "telephonePro": "+32 470 55 66 77", "motDePasse": "provisoire-2026"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void unCompteDesactiveNePeutPlusSeConnecterPuisLePeutANouveau() throws Exception {
        String admin = connecter(GESTIONNAIRE, "password");
        int membre = corps(mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Test\",\"prenom\":\"Bloque\",\"email\":\"bloque@test.immoconnect.be\",\"motDePasse\":\"motdepasse123\"}"))
                .andExpect(status().isCreated())).get("utilisateur").get("id").asInt();

        mvc.perform(patch("/api/v1/admin/utilisateurs/" + membre + "/desactiver").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actif").value(false));
        // Même réponse générique que pour un mauvais mot de passe : rien ne dit que le compte existe
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bloque@test.immoconnect.be\",\"motDePasse\":\"motdepasse123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Identifiants invalides"));

        mvc.perform(patch("/api/v1/admin/utilisateurs/" + membre + "/activer").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actif").value(true));
        connecter("bloque@test.immoconnect.be", "motdepasse123");
    }

    @Test
    void lesNiveauxDAccesLimitentCeQueChaqueAdministrateurPeutFaire() throws Exception {
        String editeur = connecter(EDITEUR, "password");
        String gestionnaire = connecter(GESTIONNAIRE, "password");

        // L'éditeur gère les catégories, pas les comptes ni le journal
        mvc.perform(get("/api/v1/admin/utilisateurs").header("Authorization", "Bearer " + editeur)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/journal").header("Authorization", "Bearer " + editeur)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/statistiques").header("Authorization", "Bearer " + editeur)).andExpect(status().isForbidden());

        // Le gestionnaire ne touche pas au compte d'un administrateur, et nul ne se désactive lui-même
        mvc.perform(patch("/api/v1/admin/utilisateurs/" + SUPER_ADMIN_ID + "/desactiver").header("Authorization", "Bearer " + gestionnaire))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/admin/utilisateurs/" + GESTIONNAIRE_ID + "/desactiver").header("Authorization", "Bearer " + gestionnaire))
                .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM utilisateur WHERE id IN (?, ?) AND actif = 1", Integer.class,
                SUPER_ADMIN_ID, GESTIONNAIRE_ID)).isEqualTo(2);
    }

    @Test
    void leBackOfficeEstFermeAuxAutresRoles() throws Exception {
        mvc.perform(get("/api/v1/admin/utilisateurs")).andExpect(status().isUnauthorized());
        for (String compte : new String[] {"alice.benali@mail.be", "sarah.dubois@mail.be"}) {
            String jeton = connecter(compte, "password");
            mvc.perform(get("/api/v1/admin/utilisateurs").header("Authorization", "Bearer " + jeton)).andExpect(status().isForbidden());
            mvc.perform(get("/api/v1/admin/journal").header("Authorization", "Bearer " + jeton)).andExpect(status().isForbidden());
            mvc.perform(post("/api/v1/admin/categories").header("Authorization", "Bearer " + jeton)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"nom\":\"Piratage\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void laListeDesComptesSeFiltreParRoleEtParTexte() throws Exception {
        String admin = connecter(SUPER_ADMIN, "password");

        mvc.perform(get("/api/v1/admin/utilisateurs").param("role", "agent").param("taille", "100").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu[*].role", Matchers.everyItem(Matchers.is("agent"))))
                .andExpect(jsonPath("$.totalElements", Matchers.greaterThanOrEqualTo(8)));
        mvc.perform(get("/api/v1/admin/utilisateurs").param("recherche", "sarah.dubois").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.contenu[0].matricule").value("AG-2023-001"));
        // Une saisie hostile reste un simple texte recherché
        mvc.perform(get("/api/v1/admin/utilisateurs").param("recherche", "' OR '1'='1").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/admin/utilisateurs").param("role", "pirate").header("Authorization", "Bearer " + admin))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void leJournalDAuditSeConsulteDuPlusRecentAuPlusAncienEtSeFiltre() throws Exception {
        String admin = connecter(SUPER_ADMIN, "password");

        mvc.perform(get("/api/v1/admin/journal").param("taille", "5").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu", Matchers.hasSize(5)))
                // La connexion de l'administrateur vient d'être journalisée
                .andExpect(jsonPath("$.contenu[0].action").value("connexion"))
                .andExpect(jsonPath("$.contenu[0].utilisateur").value("David Moreau"))
                .andExpect(jsonPath("$.contenu[0].ip").isNotEmpty());
        mvc.perform(get("/api/v1/admin/journal").param("action", "connexion").param("utilisateurId", String.valueOf(SUPER_ADMIN_ID))
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu[*].action", Matchers.everyItem(Matchers.is("connexion"))))
                .andExpect(jsonPath("$.contenu[*].utilisateurId", Matchers.everyItem(Matchers.is(SUPER_ADMIN_ID))));
        mvc.perform(get("/api/v1/admin/journal").param("du", "2025-01-01").param("au", "2025-01-31").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu[*].horodatage", Matchers.everyItem(Matchers.startsWith("2025-01"))));
        mvc.perform(get("/api/v1/admin/journal/actions").header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$", Matchers.hasItems("connexion", "creation_rdv", "paiement_reussi")));

        // Aucune trace n'est datée dans le futur
        LocalDateTime maintenant = LocalDateTime.now(ConfigurationHorloge.FUSEAU_AGENCE).plusMinutes(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_audit WHERE horodatage > ?", Integer.class, maintenant)).isZero();
    }

    @Test
    void lesStatistiquesAgregentLActiviteDuSite() throws Exception {
        mvc.perform(get("/api/v1/admin/statistiques").header("Authorization", "Bearer " + connecter(GESTIONNAIRE, "password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.biensParStatut.disponible", Matchers.greaterThan(50)))
                .andExpect(jsonPath("$.comptesParRole.membre", Matchers.greaterThanOrEqualTo(100)))
                .andExpect(jsonPath("$.comptesParRole.admin").value(3))
                .andExpect(jsonPath("$.rendezVousParStatut.honore", Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.revenusPremium", Matchers.greaterThan(0.0)))
                .andExpect(jsonPath("$.communes", Matchers.hasSize(8)))
                .andExpect(jsonPath("$.communes[0].prixMoyen", Matchers.greaterThan(0)));
    }

    @Test
    void uneCategorieSeCreeSeModifieEtNeSeSupprimeQueSiElleEstVide() throws Exception {
        String editeur = connecter(EDITEUR, "password");

        int id = corps(mvc.perform(post("/api/v1/admin/categories").header("Authorization", "Bearer " + editeur)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Kot étudiant\",\"description\":\"Chambres et studios pour étudiants.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nom").value("Kot étudiant"))).get("id").asInt();
        mvc.perform(post("/api/v1/admin/categories").header("Authorization", "Bearer " + editeur)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nom\":\"kot étudiant\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.champs.nom").isNotEmpty());

        mvc.perform(put("/api/v1/admin/categories/" + id).header("Authorization", "Bearer " + editeur)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nom\":\"Logement étudiant\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Logement étudiant"))
                .andExpect(jsonPath("$.description").isEmpty());
        mvc.perform(get("/api/v1/categories"))
                .andExpect(jsonPath("$[*].nom", Matchers.hasItem("Logement étudiant")));

        mvc.perform(delete("/api/v1/admin/categories/" + id).header("Authorization", "Bearer " + editeur))
                .andExpect(status().isNoContent());
        // « Appartement » est portée par des biens
        mvc.perform(delete("/api/v1/admin/categories/2").header("Authorization", "Bearer " + editeur))
                .andExpect(status().isConflict());
    }

    private JsonNode corps(ResultActions reponse) throws Exception {
        return json.readTree(reponse.andReturn().getResponse().getContentAsString());
    }

    private String connecter(String email, String motDePasse) throws Exception {
        return corps(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"motDePasse\":\"" + motDePasse + "\"}"))
                .andExpect(status().isOk())).get("jeton").asString();
    }
}
