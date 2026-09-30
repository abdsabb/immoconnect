package be.immoconnect.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import be.immoconnect.TestcontainersConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
 * Scénario « Prendre rendez-vous pour visiter un bien » sur un créneau standard (alternative A1),
 * créneau déjà pris (A2), bien retiré de la vente (E3) et contrôle d'accès, sur un vrai MySQL 8.4.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class RendezVousControleurTest {

    /** Bien disponible de l'agente Sarah Dubois (101) ; le bien 1 est vendu. */
    private static final int BIEN = 9;
    private static final int BIEN_VENDU = 1;
    private static final int AGENTE = 101;
    private static final String MEMBRE = "alice.benali@mail.be";
    private static final String AGENTE_EMAIL = "sarah.dubois@mail.be";
    private static final String AUTRE_AGENT = "marc.peeters@mail.be";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void lesCreneauxLibresDistinguentStandardEtPremium() throws Exception {
        mvc.perform(get("/api/v1/biens/" + BIEN + "/creneaux").header("Authorization", "Bearer " + connecter(MEMBRE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.type == 'standard')].prix", Matchers.everyItem(Matchers.is(0))))
                .andExpect(jsonPath("$[?(@.type == 'premium')].prix", Matchers.everyItem(Matchers.is(15.0))))
                .andExpect(jsonPath("$[?(@.type == 'standard')]", Matchers.not(Matchers.empty())))
                .andExpect(jsonPath("$[?(@.type == 'premium')]", Matchers.not(Matchers.empty())));
    }

    @Test
    void lesCreneauxSontReservesAuxMembres() throws Exception {
        mvc.perform(get("/api/v1/biens/" + BIEN + "/creneaux"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/biens/" + BIEN + "/creneaux").header("Authorization", "Bearer " + connecter(AGENTE_EMAIL)))
                .andExpect(status().isForbidden());
    }

    @Test
    void leCycleCompletDUnCreneauStandard() throws Exception {
        String membre = connecter(MEMBRE);
        String agente = connecter(AGENTE_EMAIL);
        String creneau = premierCreneau(membre, "standard");

        // A1 — le membre réserve : le rendez-vous attend la confirmation, l'adresse reste masquée.
        JsonNode cree = corps(reserver(membre, BIEN, creneau, "Visite avec mon architecte")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("demande"))
                .andExpect(jsonPath("$.type").value("standard"))
                .andExpect(jsonPath("$.paiement").isEmpty())
                .andExpect(jsonPath("$.adresse").isEmpty())
                .andExpect(jsonPath("$.agent").value("Sarah Dubois")));
        int id = cree.get("id").asInt();
        assertThat(creneauxLibres(membre)).doesNotContain(creneau);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_audit WHERE action = 'creation_rdv' AND entite = ?",
                Integer.class, "rendez_vous#" + id)).isEqualTo(1);

        // A2 — un autre membre arrive trop tard sur le même créneau.
        reserver(connecter(emailDuMembre(2)), BIEN, creneau, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://www.immoconnect.be/erreurs/creneau-indisponible"));

        // Contrôle d'accès : un membre ne confirme pas, un autre agent non plus.
        mvc.perform(patch("/api/v1/rendez-vous/" + id + "/confirmer").header("Authorization", "Bearer " + membre))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/rendez-vous/" + id + "/confirmer").header("Authorization", "Bearer " + connecter(AUTRE_AGENT)))
                .andExpect(status().isForbidden());

        // L'agente du bien confirme : le membre découvre l'adresse exacte.
        mvc.perform(patch("/api/v1/rendez-vous/" + id + "/confirmer").header("Authorization", "Bearer " + agente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("confirme"));
        mvc.perform(get("/api/v1/rendez-vous").header("Authorization", "Bearer " + membre))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")].adresse", Matchers.contains(Matchers.containsString("Anderlecht"))));
        mvc.perform(get("/api/v1/rendez-vous").header("Authorization", "Bearer " + agente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")].membre", Matchers.contains("Alice Benali")));

        // RA2 — la visite n'a pas encore eu lieu : elle ne peut pas être honorée.
        mvc.perform(patch("/api/v1/rendez-vous/" + id + "/honorer").header("Authorization", "Bearer " + agente))
                .andExpect(status().isConflict());

        // Le membre annule : le créneau retourne dans la grille ; RA1 interdit d'annuler deux fois.
        mvc.perform(patch("/api/v1/rendez-vous/" + id + "/annuler").header("Authorization", "Bearer " + membre))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("annule"));
        mvc.perform(patch("/api/v1/rendez-vous/" + id + "/annuler").header("Authorization", "Bearer " + membre))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://www.immoconnect.be/erreurs/conflit-etat"));
        assertThat(creneauxLibres(membre)).contains(creneau);
    }

    /** A2 en conditions réelles : six membres visent le même créneau au même instant, un seul l'obtient. */
    @Test
    void deSixReservationsSimultaneesUneSeuleAboutit() throws Exception {
        String creneau = premierCreneau(connecter(MEMBRE), "standard");
        List<String> jetons = new ArrayList<>();
        for (int membre = 10; membre < 16; membre++) {
            jetons.add(connecter(emailDuMembre(membre)));
        }

        CountDownLatch depart = new CountDownLatch(1);
        List<Future<Integer>> reponses = new ArrayList<>();
        try (ExecutorService fils = Executors.newFixedThreadPool(jetons.size())) {
            for (String jeton : jetons) {
                reponses.add(fils.submit(() -> {
                    depart.await();
                    return reserver(jeton, BIEN, creneau, null).andReturn().getResponse().getStatus();
                }));
            }
            depart.countDown();
        }

        List<Integer> statuts = new ArrayList<>();
        for (Future<Integer> reponse : reponses) {
            statuts.add(reponse.get());
        }
        assertThat(statuts).containsOnly(201, 409);
        assertThat(statuts).filteredOn(statut -> statut == 201).hasSize(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM rendez_vous WHERE agent_id = ? AND date_heure = ? "
                + "AND statut IN ('demande', 'confirme')", Integer.class, AGENTE, creneau.replace('T', ' '))).isEqualTo(1);
    }

    @Test
    void unMembreNAnnulePasLeRendezVousDUnAutre() throws Exception {
        String membre = connecter(MEMBRE);
        int id = corps(reserver(membre, BIEN, premierCreneau(membre, "standard"), null).andExpect(status().isCreated()))
                .get("id").asInt();

        mvc.perform(patch("/api/v1/rendez-vous/" + id + "/annuler").header("Authorization", "Bearer " + connecter(emailDuMembre(3))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://www.immoconnect.be/erreurs/interdit"));
    }

    @Test
    void unCreneauHorsGrilleOuPasseEstRefuse() throws Exception {
        String membre = connecter(MEMBRE);
        String horsGrille = premierCreneau(membre, "standard").substring(0, 11) + "12:15:00";

        reserver(membre, BIEN, horsGrille, null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.champs.dateHeure").isNotEmpty());
        reserver(membre, BIEN, "2026-01-05T10:30:00", null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.champs.dateHeure").isNotEmpty());
    }

    @Test
    void unCreneauPremiumNeSeReservePasSansPaiement() throws Exception {
        String membre = connecter(MEMBRE);

        reserver(membre, BIEN, premierCreneau(membre, "premium"), null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.champs.paymentIntentId").isNotEmpty());
    }

    @Test
    void unBienVenduOuInexistantNePrendPlusDeRendezVous() throws Exception {
        String membre = connecter(MEMBRE);
        String creneau = premierCreneau(membre, "standard");

        reserver(membre, BIEN_VENDU, creneau, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://www.immoconnect.be/erreurs/bien-indisponible"));
        reserver(membre, 999999, creneau, null)
                .andExpect(status().isNotFound());
    }

    @Test
    void lAgenteHonoreUneVisitePassee() throws Exception {
        Integer id = jdbc.queryForObject("SELECT MIN(id) FROM rendez_vous WHERE agent_id = ? AND statut = 'confirme' "
                + "AND date_heure < '2026-09-01'", Integer.class, AGENTE);

        mvc.perform(patch("/api/v1/rendez-vous/" + id + "/honorer").header("Authorization", "Bearer " + connecter(AGENTE_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("honore"));
    }

    private ResultActions reserver(String jeton, int bienId, String dateHeure, String motif) throws Exception {
        String corps = "{\"bienId\":" + bienId + ",\"dateHeure\":\"" + dateHeure + "\""
                + (motif == null ? "" : ",\"motif\":\"" + motif + "\"") + "}";
        return mvc.perform(post("/api/v1/rendez-vous").header("Authorization", "Bearer " + jeton)
                .contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    private String premierCreneau(String jeton, String type) throws Exception {
        for (JsonNode creneau : creneaux(jeton)) {
            if (type.equals(creneau.get("type").asString())) {
                return creneau.get("dateHeure").asString();
            }
        }
        throw new AssertionError("Aucun créneau " + type + " libre");
    }

    private List<String> creneauxLibres(String jeton) throws Exception {
        return creneaux(jeton).valueStream().map(c -> c.get("dateHeure").asString()).toList();
    }

    private JsonNode creneaux(String jeton) throws Exception {
        return corps(mvc.perform(get("/api/v1/biens/" + BIEN + "/creneaux").header("Authorization", "Bearer " + jeton))
                .andExpect(status().isOk()));
    }

    private JsonNode corps(ResultActions reponse) throws Exception {
        return json.readTree(reponse.andReturn().getResponse().getContentAsString());
    }

    private String emailDuMembre(int id) {
        return jdbc.queryForObject("SELECT email FROM utilisateur WHERE id = ? AND role = 'membre'", String.class, id);
    }

    private String connecter(String email) throws Exception {
        return corps(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"motDePasse\":\"password\"}"))
                .andExpect(status().isOk())).get("jeton").asString();
    }
}
