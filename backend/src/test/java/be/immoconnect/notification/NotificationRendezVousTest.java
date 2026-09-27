package be.immoconnect.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import be.immoconnect.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Notifications par e-mail des rendez-vous (pattern Observer) : bon destinataire, bonne langue,
 * et cas d'erreur E2 — un serveur SMTP en panne n'empêche jamais de prendre rendez-vous.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "immoconnect.courriel.delai-entre-tentatives=10ms")
@AutoConfigureMockMvc
class NotificationRendezVousTest {

    /** Bien disponible de l'agente Aurore Fontaine (107). */
    private static final int BIEN = 3;
    private static final String AGENTE = "aurore.fontaine@mail.be";
    private static final int ATTENTE_MS = 5000;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @MockitoBean
    private JavaMailSender messagerie;

    @BeforeEach
    void oublierLesEnvoisPrecedents() {
        reset(messagerie);
    }

    @Test
    void uneDemandePrevientLAgentPuisSaConfirmationPrevientLeMembreChacunDansSaLangue() throws Exception {
        String membre = inscrire("Janssens", "Emma", "emma.janssens@test.immoconnect.be", "en");
        int id = corps(reserver(membre, premierCreneauStandard(membre)).andExpect(status().isCreated())).get("id").asInt();

        // L'agente a choisi le néerlandais dans son profil
        SimpleMailMessage pourLAgente = dernierEnvoi();
        assertThat(pourLAgente.getTo()).containsExactly(AGENTE);
        assertThat(pourLAgente.getSubject()).startsWith("[ImmoConnect] Nieuwe bezoekaanvraag");
        assertThat(pourLAgente.getText()).contains("Dag Aurore", "Emma Janssens wil", "Reden: Seconde visite");

        reset(messagerie);
        mvc.perform(patch("/api/v1/rendez-vous/" + id + "/confirmer").header("Authorization", "Bearer " + connecter(AGENTE)))
                .andExpect(status().isOk());

        // Le membre s'est inscrit en anglais : il reçoit l'adresse exacte du bien
        SimpleMailMessage pourLeMembre = dernierEnvoi();
        assertThat(pourLeMembre.getTo()).containsExactly("emma.janssens@test.immoconnect.be");
        assertThat(pourLeMembre.getSubject()).startsWith("[ImmoConnect] Your visit is confirmed");
        assertThat(pourLeMembre.getText()).contains("Hello Emma", "Address: Rue Antoine Dansaert 88, 1180 Uccle",
                "Your agent: Aurore Fontaine");
    }

    @Test
    void lAnnulationParLeMembrePrevientLAgent() throws Exception {
        String membre = inscrire("Lefebvre", "Hugo", "hugo.lefebvre@test.immoconnect.be", "fr");
        int id = corps(reserver(membre, premierCreneauStandard(membre)).andExpect(status().isCreated())).get("id").asInt();
        dernierEnvoi();

        reset(messagerie);
        mvc.perform(patch("/api/v1/rendez-vous/" + id + "/annuler").header("Authorization", "Bearer " + membre))
                .andExpect(status().isOk());

        SimpleMailMessage pourLAgente = dernierEnvoi();
        assertThat(pourLAgente.getTo()).containsExactly(AGENTE);
        assertThat(pourLAgente.getSubject()).startsWith("[ImmoConnect] Bezoek geannuleerd");
        assertThat(pourLAgente.getText()).contains("Hugo Lefebvre heeft het bezoek");
    }

    /** E2 : l'envoi échoue, il est retenté trois fois, et le rendez-vous reste valide. */
    @Test
    void unePanneDeMessagerieNEmpechePasLaPriseDeRendezVous() throws Exception {
        doThrow(new MailSendException("serveur SMTP injoignable")).when(messagerie).send(any(SimpleMailMessage.class));
        String membre = inscrire("Maes", "Lotte", "lotte.maes@test.immoconnect.be", "nl");

        int id = corps(reserver(membre, premierCreneauStandard(membre))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("demande"))).get("id").asInt();

        verify(messagerie, timeout(ATTENTE_MS).times(3)).send(any(SimpleMailMessage.class));
        mvc.perform(get("/api/v1/rendez-vous").header("Authorization", "Bearer " + membre))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].statut").value("demande"));
    }

    private SimpleMailMessage dernierEnvoi() {
        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(messagerie, timeout(ATTENTE_MS)).send(message.capture());
        return message.getValue();
    }

    private ResultActions reserver(String jeton, String dateHeure) throws Exception {
        return mvc.perform(post("/api/v1/rendez-vous").header("Authorization", "Bearer " + jeton)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bienId\":" + BIEN + ",\"dateHeure\":\"" + dateHeure + "\",\"motif\":\"Seconde visite\"}"));
    }

    private String premierCreneauStandard(String jeton) throws Exception {
        return corps(mvc.perform(get("/api/v1/biens/" + BIEN + "/creneaux").header("Authorization", "Bearer " + jeton))
                .andExpect(status().isOk())).valueStream()
                .filter(c -> "standard".equals(c.get("type").asString()))
                .findFirst().orElseThrow().get("dateHeure").asString();
    }

    private String inscrire(String nom, String prenom, String email, String langue) throws Exception {
        return corps(mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"" + nom + "\",\"prenom\":\"" + prenom + "\",\"email\":\"" + email
                                + "\",\"motDePasse\":\"motdepasse123\",\"langue\":\"" + langue + "\"}"))
                .andExpect(status().isCreated())).get("jeton").asString();
    }

    private String connecter(String email) throws Exception {
        return corps(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"motDePasse\":\"password\"}"))
                .andExpect(status().isOk())).get("jeton").asString();
    }

    private JsonNode corps(ResultActions reponse) throws Exception {
        return json.readTree(reponse.andReturn().getResponse().getContentAsString());
    }
}
