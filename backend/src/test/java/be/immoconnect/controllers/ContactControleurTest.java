package be.immoconnect.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import be.immoconnect.CourrielRecu;
import be.immoconnect.TestcontainersConfiguration;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Formulaire de contact : enregistrement, e-mail à l'agence, parades contre l'abus, suivi par l'administrateur. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ContactControleurTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private JavaMailSender messagerie;

    @Test
    void unVisiteurEcritALAgenceQuiRecoitLeMessageEtLeTraite() throws Exception {
        envoyer("198.51.100.20", """
                {"nom":"Élise Martin","email":"elise.martin@mail.be","telephone":"+32 470 12 34 56",
                 "sujet":"Estimation d'une maison","message":"Bonjour, proposez-vous une estimation à Uccle ?"}
                """).andExpect(status().isAccepted());

        // L'agence est prévenue par e-mail, à l'adresse réglée dans les paramètres du site
        ArgumentCaptor<MimeMessage> envoi = ArgumentCaptor.forClass(MimeMessage.class);
        verify(messagerie, timeout(5000)).send(envoi.capture());
        CourrielRecu courriel = CourrielRecu.de(envoi.getValue());
        assertThat(courriel.destinataires()).containsExactly("contact@immoconnect.be");
        assertThat(courriel.sujet()).contains("Estimation d'une maison");
        assertThat(courriel.texte()).contains("Élise Martin", "elise.martin@mail.be", "estimation à Uccle");
        // Version HTML : ce que le visiteur a saisi y entre échappé, jamais comme balise ni comme lien
        assertThat(courriel.html()).contains("<strong>De</strong> : Élise Martin", "estimation à Uccle ?").doesNotContain("<script");

        // Le gestionnaire la retrouve dans le back-office et la marque comme traitée ; l'éditeur n'y a pas accès
        String gestionnaire = connecter("lotte.goossens@mail.be");
        JsonNode liste = corps(mvc.perform(get("/api/v1/admin/contacts").param("enAttente", "true").header("Authorization", "Bearer " + gestionnaire))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu[0].nom").value("Élise Martin"))
                .andExpect(jsonPath("$.contenu[0].traitee").value(false)));
        int id = liste.get("contenu").get(0).get("id").asInt();
        mvc.perform(get("/api/v1/admin/contacts").header("Authorization", "Bearer " + connecter("yasmine.benali@mail.be")))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/admin/contacts/" + id + "/traiter").header("Authorization", "Bearer " + gestionnaire))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.traitee").value(true))
                .andExpect(jsonPath("$.traitePar").value("Lotte Goossens"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_audit WHERE action = 'traitement_contact' AND entite = ?",
                Integer.class, "demande_contact#" + id)).isEqualTo(1);
    }

    @Test
    void unFormulaireIncompletEstRefuseChampParChamp() throws Exception {
        envoyer("198.51.100.21", "{\"nom\":\" \",\"email\":\"pas-une-adresse\",\"sujet\":\"\",\"message\":\"\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.champs.nom").isNotEmpty())
                .andExpect(jsonPath("$.champs.email").isNotEmpty())
                .andExpect(jsonPath("$.champs.sujet").isNotEmpty())
                .andExpect(jsonPath("$.champs.message").isNotEmpty());
    }

    @Test
    void lesRobotsEtLesEnvoisEnRafaleSontEcartes() throws Exception {
        // Champ piège rempli : la réponse est la même, mais rien n'est enregistré
        envoyer("198.51.100.22", "{\"nom\":\"Robot\",\"email\":\"robot@spam.test\",\"sujet\":\"Offre\",\"message\":\"Achetez\",\"site\":\"http://spam.test\"}")
                .andExpect(status().isAccepted());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM demande_contact WHERE email = 'robot@spam.test'", Integer.class)).isZero();

        // Cinq messages par heure et par adresse IP ; le sixième attend
        String message = "{\"nom\":\"Insistant\",\"email\":\"insistant@mail.be\",\"sujet\":\"Encore\",\"message\":\"Bonjour\"}";
        for (int i = 0; i < 5; i++) {
            envoyer("198.51.100.23", message).andExpect(status().isAccepted());
        }
        envoyer("198.51.100.23", message).andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM demande_contact WHERE email = 'insistant@mail.be'", Integer.class)).isEqualTo(5);
    }

    private ResultActions envoyer(String ip, String corps) throws Exception {
        return mvc.perform(post("/api/v1/contact").header("X-Forwarded-For", ip).contentType(MediaType.APPLICATION_JSON).content(corps));
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
