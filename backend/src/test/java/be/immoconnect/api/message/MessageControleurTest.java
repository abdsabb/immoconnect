package be.immoconnect.api.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

/** Messagerie (M3, AG4) : échange membre ↔ agent, messages non lus, cloisonnement des conversations. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class MessageControleurTest {

    private static final int AGENT = 102;
    private static final String AGENT_EMAIL = "marc.peeters@mail.be";
    private static final int AUTRE_AGENT = 104;
    private static final String AUTRE_AGENT_EMAIL = "julien.lambert@mail.be";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void unMembreEcritLAgentRepondEtChacunVoitSesMessagesNonLus() throws Exception {
        JsonNode membre = inscrire("messagerie.un@test.immoconnect.be", "Nora");
        String jetonMembre = membre.get("jeton").asString();
        int membreId = membre.get("utilisateur").get("id").asInt();
        String jetonAgent = connecter(AGENT_EMAIL);

        envoyer(jetonMembre, AGENT, "Bonjour, la cuisine est-elle équipée ?")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.expediteur").value("Nora Test"))
                .andExpect(jsonPath("$.destinataire").value("Marc Peeters"))
                .andExpect(jsonPath("$.deMoi").value(true))
                .andExpect(jsonPath("$.lu").value(false));

        // L'agent voit la conversation en tête de liste, avec un message à lire
        mvc.perform(get("/api/v1/messages").header("Authorization", "Bearer " + jetonAgent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].interlocuteurId").value(membreId))
                .andExpect(jsonPath("$[0].interlocuteur").value("Nora Test"))
                .andExpect(jsonPath("$[0].nonLus").value(1))
                .andExpect(jsonPath("$[0].dernierDeMoi").value(false));

        // Il l'ouvre, puis répond
        mvc.perform(patch("/api/v1/messages/conversations/" + membreId + "/lu").header("Authorization", "Bearer " + jetonAgent))
                .andExpect(status().isNoContent());
        envoyer(jetonAgent, membreId, "Bonjour, oui : four, taque et lave-vaisselle.")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.expediteur").value("Marc Peeters"));

        // Le membre retrouve l'échange dans l'ordre, sa question lue et la réponse à lire
        mvc.perform(get("/api/v1/messages/conversations/" + AGENT).header("Authorization", "Bearer " + jetonMembre))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].deMoi").value(true))
                .andExpect(jsonPath("$[0].lu").value(true))
                .andExpect(jsonPath("$[1].deMoi").value(false))
                .andExpect(jsonPath("$[1].lu").value(false));
        mvc.perform(get("/api/v1/messages").header("Authorization", "Bearer " + jetonMembre))
                .andExpect(jsonPath("$", Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].nonLus").value(1))
                .andExpect(jsonPath("$[0].dernierMessage").value("Bonjour, oui : four, taque et lave-vaisselle."));

        // Marquer la conversation comme lue ne touche jamais aux messages que l'on a écrits
        mvc.perform(patch("/api/v1/messages/conversations/" + AGENT + "/lu").header("Authorization", "Bearer " + jetonMembre))
                .andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM message WHERE membre_id = ? AND lu = 0", Integer.class, membreId)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_audit WHERE utilisateur_id = ? AND action = 'envoi_message'",
                Integer.class, membreId)).isEqualTo(1);
    }

    @Test
    void uneConversationNEstVisibleQueDeSesDeuxParties() throws Exception {
        JsonNode membre = inscrire("messagerie.deux@test.immoconnect.be", "Samir");
        int membreId = membre.get("utilisateur").get("id").asInt();
        envoyer(membre.get("jeton").asString(), AGENT, "Bonjour, le bien est-il encore disponible ?").andExpect(status().isCreated());

        // Un autre agent interroge la même conversation : il ne voit que la sienne, qui est vide
        mvc.perform(get("/api/v1/messages/conversations/" + membreId).header("Authorization", "Bearer " + connecter(AUTRE_AGENT_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.empty()));
        // Un autre membre non plus
        String intrus = inscrire("messagerie.trois@test.immoconnect.be", "Intrus").get("jeton").asString();
        mvc.perform(get("/api/v1/messages/conversations/" + AGENT).header("Authorization", "Bearer " + intrus))
                .andExpect(jsonPath("$", Matchers.empty()));
    }

    @Test
    void unAgentNeDemarchePasUnMembreQuiNeLuiAPasEcrit() throws Exception {
        int membreId = inscrire("messagerie.quatre@test.immoconnect.be", "Lina").get("utilisateur").get("id").asInt();

        envoyer(connecter(AUTRE_AGENT_EMAIL), membreId, "Bonjour, j'ai un bien qui pourrait vous plaire.")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://www.immoconnect.be/erreurs/interdit"));
    }

    @Test
    void unMessageVideOuSansDestinataireValideEstRefuse() throws Exception {
        String membre = inscrire("messagerie.cinq@test.immoconnect.be", "Yanis").get("jeton").asString();

        envoyer(membre, AGENT, "   ")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.champs.contenu").isNotEmpty());
        // Un membre n'écrit qu'à un agent : ni à un autre membre, ni à un administrateur
        envoyer(membre, 1, "Bonjour").andExpect(status().isNotFound());
        envoyer(membre, 109, "Bonjour").andExpect(status().isNotFound());
    }

    @Test
    void laMessagerieEstReserveeAuxMembresEtAuxAgents() throws Exception {
        mvc.perform(get("/api/v1/messages")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/messages").header("Authorization", "Bearer " + connecter("david.moreau@mail.be")))
                .andExpect(status().isForbidden());
    }

    @Test
    void lesDonneesDeTestContiennentDesReponsesDAgents() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM message WHERE expediteur = 'agent'", Integer.class)).isGreaterThan(100);
        // Comparé à l'heure de l'agence, pas à NOW() : le serveur MySQL du test tourne en UTC
        LocalDateTime maintenant = LocalDateTime.now(ConfigurationHorloge.FUSEAU_AGENCE).plusMinutes(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM message WHERE envoye_le > ?", Integer.class, maintenant)).isZero();
    }

    private ResultActions envoyer(String jeton, int destinataireId, String contenu) throws Exception {
        return mvc.perform(post("/api/v1/messages").header("Authorization", "Bearer " + jeton)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new RequeteMessage(destinataireId, contenu))));
    }

    private JsonNode inscrire(String email, String prenom) throws Exception {
        return json.readTree(mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Test\",\"prenom\":\"" + prenom + "\",\"email\":\"" + email + "\",\"motDePasse\":\"motdepasse123\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private String connecter(String email) throws Exception {
        return json.readTree(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"motDePasse\":\"password\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("jeton").asString();
    }
}
