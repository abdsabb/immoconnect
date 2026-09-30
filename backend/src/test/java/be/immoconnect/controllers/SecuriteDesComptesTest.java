package be.immoconnect.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockingDetails;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import be.immoconnect.TestcontainersConfiguration;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Sécurité des comptes (livrable 16, §2), dans la configuration de production : activation par
 * e-mail, double facteur, verrouillage, session par cookie, mot de passe oublié. Les e-mails sont
 * capturés par un faux serveur d'envoi.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "immoconnect.securite.activation-par-courriel=true",
        "immoconnect.securite.double-facteur=true",
        "immoconnect.securite.echecs-avant-verrouillage=3",
        "immoconnect.securite.connexions-par-minute=1000",
})
class SecuriteDesComptesTest {

    private static final String MOT_DE_PASSE = "Visite-Bxl-2026";
    private static final String AGENT = "sarah.dubois@mail.be";
    private static final Pattern JETON_DU_LIEN = Pattern.compile("jeton=([A-Za-z0-9_-]+)");
    private static final Pattern CODE = Pattern.compile("code de connexion est : ([0-9]{6})");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private JavaMailSender messagerie;

    @Test
    void unNouveauCompteSeConnecteApresAvoirConfirmeSonAdresse() throws Exception {
        String email = "activation@test.immoconnect.be";
        int avant = courriels(email).size();
        mvc.perform(inscription(email))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activationRequise").value(true))
                .andExpect(jsonPath("$.jeton").isEmpty())
                .andExpect(header().doesNotExist("Set-Cookie"));

        // Tant que l'adresse n'est pas confirmée, le bon mot de passe ne suffit pas
        mvc.perform(connexion(email, MOT_DE_PASSE))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://www.immoconnect.be/erreurs/compte-non-active"));

        String lien = extraire(courrielSuivant(email, avant), JETON_DU_LIEN);
        MvcResult activation = mvc.perform(activation(lien))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jeton").isNotEmpty())
                .andExpect(cookie().httpOnly(AuthControleur.COOKIE, true))
                .andExpect(cookie().path(AuthControleur.COOKIE, "/api/v1/auth"))
                .andReturn();
        assertThat(activation.getResponse().getHeader("Set-Cookie")).contains("SameSite=Strict");

        // Un lien ne sert qu'une fois
        mvc.perform(activation(lien))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://www.immoconnect.be/erreurs/lien-invalide"));
        assertThat(jdbc.queryForObject("select email_verifie from utilisateur where email = ?", Boolean.class, email)).isTrue();
        mvc.perform(connexion(email, MOT_DE_PASSE)).andExpect(status().isOk());
    }

    @Test
    void unAgentEntreLeCodeRecuParCourriel() throws Exception {
        int avant = courriels(AGENT).size();
        String defi = corps(mvc.perform(connexion(AGENT, "password"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.doubleFacteur").value(true))
                .andExpect(header().doesNotExist("Set-Cookie"))).get("defi").asString();
        String code = extraire(courrielSuivant(AGENT, avant), CODE);

        mvc.perform(code(defi, "000000"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("https://www.immoconnect.be/erreurs/code-invalide"))
                .andExpect(jsonPath("$.essaisRestants").value(4));
        mvc.perform(code(defi, code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.utilisateur.role").value("agent"))
                .andExpect(cookie().exists(AuthControleur.COOKIE));
        // Le code, lui non plus, ne sert qu'une fois
        mvc.perform(code(defi, code)).andExpect(status().isBadRequest());
    }

    @Test
    void troisEchecsVerrouillentLeCompteQuIlExisteOuNon() throws Exception {
        for (String email : new String[] {"alice.benali@mail.be", "personne@test.immoconnect.be"}) {
            for (int i = 0; i < 3; i++) {
                mvc.perform(connexion(email, "faux-" + i)).andExpect(status().isUnauthorized());
            }
            mvc.perform(connexion(email, "password"))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(header().exists("Retry-After"))
                    .andExpect(jsonPath("$.type").value("https://www.immoconnect.be/erreurs/trop-de-tentatives"));
        }
        assertThat(jdbc.queryForObject("select count(*) from journal_audit where action = 'echec_connexion' "
                + "and utilisateur_id = (select id from utilisateur where email = 'alice.benali@mail.be')", Integer.class)).isEqualTo(3);
        // Le verrou a une fin : il est levé ici pour vérifier qu'une connexion réussie remet le compteur à zéro
        jdbc.update("update utilisateur set verrouille_jusqu_a = null where email = 'alice.benali@mail.be'");
        mvc.perform(connexion("alice.benali@mail.be", "password")).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select echecs_connexion from utilisateur where email = 'alice.benali@mail.be'", Integer.class)).isZero();
    }

    @Test
    void leCookieDeSessionRenouvelleLeJetonEtSeRemplaceAChaqueUsage() throws Exception {
        Cookie session = inscriptionActivee("session@test.immoconnect.be").getResponse().getCookie(AuthControleur.COOKIE);
        assertThat(session).isNotNull();

        MvcResult renouvele = mvc.perform(post("/api/v1/auth/refresh").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jeton").isNotEmpty())
                .andExpect(cookie().exists(AuthControleur.COOKIE))
                .andReturn();
        Cookie suivant = renouvele.getResponse().getCookie(AuthControleur.COOKIE);
        assertThat(suivant.getValue()).isNotEqualTo(session.getValue());
        String jeton = corps(renouvele).get("jeton").asString();
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + jeton)).andExpect(status().isOk());

        // Un cookie déjà utilisé trahit un vol : il est refusé et toutes les sessions se ferment
        mvc.perform(post("/api/v1/auth/refresh").cookie(session)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/auth/refresh").cookie(suivant)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/auth/refresh")).andExpect(status().isBadRequest());
    }

    @Test
    void laDeconnexionFermeLaSessionEtEffaceLeCookie() throws Exception {
        Cookie session = inscriptionActivee("logout@test.immoconnect.be").getResponse().getCookie(AuthControleur.COOKIE);

        mvc.perform(post("/api/v1/auth/logout").cookie(session))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(AuthControleur.COOKIE, 0));
        mvc.perform(post("/api/v1/auth/refresh").cookie(session)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/auth/logout")).andExpect(status().isNoContent());
    }

    @Test
    void leMotDePasseOublieSeReinitialiseParUnLienAUsageUnique() throws Exception {
        String email = "oubli@test.immoconnect.be";
        Cookie session = inscriptionActivee(email).getResponse().getCookie(AuthControleur.COOKIE);

        // Même réponse pour une adresse inconnue : rien ne révèle la base des comptes
        mvc.perform(adresse("/api/v1/auth/mot-de-passe-oublie", "inconnu@test.immoconnect.be")).andExpect(status().isAccepted());
        int avant = courriels(email).size();
        mvc.perform(adresse("/api/v1/auth/mot-de-passe-oublie", email)).andExpect(status().isAccepted());
        String jeton = extraire(courrielSuivant(email, avant), JETON_DU_LIEN);

        // Un mot de passe faible est refusé avec la raison
        mvc.perform(reinitialisation(jeton, "Motdepasse1"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.champs.nouveauMotDePasse").value(Matchers.containsString("fuite")));
        int avantConfirmation = courriels(email).size();
        mvc.perform(reinitialisation(jeton, "Nouveau-Mdp-2026")).andExpect(status().isNoContent());

        mvc.perform(connexion(email, MOT_DE_PASSE)).andExpect(status().isUnauthorized());
        mvc.perform(connexion(email, "Nouveau-Mdp-2026")).andExpect(status().isOk());
        // Toutes les sessions ouvertes avant la réinitialisation sont fermées, le lien ne sert plus
        mvc.perform(post("/api/v1/auth/refresh").cookie(session)).andExpect(status().isBadRequest());
        mvc.perform(reinitialisation(jeton, "Encore-Un-2026")).andExpect(status().isBadRequest());
        assertThat(courrielSuivant(email, avantConfirmation)).contains("toutes vos sessions ont été fermées");
    }

    @Test
    void unMembreChoisitSonDoubleFacteurUnAgentNePeutPasYRenoncer() throws Exception {
        String email = "choix2fa@test.immoconnect.be";
        String jeton = corps(inscriptionActivee(email)).get("jeton").asString();
        mvc.perform(patch("/api/v1/auth/me").header("Authorization", "Bearer " + jeton).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Test\",\"prenom\":\"Deux\",\"langue\":\"fr\",\"doubleFacteur\":true,\"consentementCommunications\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.doubleFacteur").value(true))
                .andExpect(jsonPath("$.consentementCommunications").value(true));
        mvc.perform(connexion(email, MOT_DE_PASSE)).andExpect(status().isAccepted());

        int avant = courriels(AGENT).size();
        String defi = corps(mvc.perform(connexion(AGENT, "password")).andExpect(status().isAccepted())).get("defi").asString();
        String code = extraire(courrielSuivant(AGENT, avant), CODE);
        String jetonAgent = corps(mvc.perform(code(defi, code)).andExpect(status().isOk())).get("jeton").asString();
        mvc.perform(patch("/api/v1/auth/me").header("Authorization", "Bearer " + jetonAgent).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Dubois\",\"prenom\":\"Sarah\",\"langue\":\"fr\",\"doubleFacteur\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.doubleFacteur").value(true))
                .andExpect(jsonPath("$.doubleFacteurImpose").value(true));
    }

    // ---------- aides ----------

    private static RequestBuilder inscription(String email) {
        return post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nom\":\"Test\",\"prenom\":\"Compte\",\"email\":\"" + email + "\",\"motDePasse\":\"" + MOT_DE_PASSE
                        + "\",\"cguAcceptees\":true}");
    }

    /** Inscription puis activation par le lien reçu : la réponse de l'activation, session comprise. */
    private MvcResult inscriptionActivee(String email) throws Exception {
        int avant = courriels(email).size();
        mvc.perform(inscription(email)).andExpect(status().isCreated());
        String jeton = extraire(courrielSuivant(email, avant), JETON_DU_LIEN);
        return mvc.perform(activation(jeton)).andExpect(status().isOk()).andReturn();
    }

    private static RequestBuilder activation(String jeton) {
        return post("/api/v1/auth/activation").contentType(MediaType.APPLICATION_JSON).content("{\"jeton\":\"" + jeton + "\"}");
    }

    private static RequestBuilder connexion(String email, String motDePasse) {
        return post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"motDePasse\":\"" + motDePasse + "\"}");
    }

    private static RequestBuilder code(String defi, String code) {
        return post("/api/v1/auth/login/code").contentType(MediaType.APPLICATION_JSON)
                .content("{\"defi\":\"" + defi + "\",\"code\":\"" + code + "\"}");
    }

    private static RequestBuilder adresse(String chemin, String email) {
        return post(chemin).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + email + "\"}");
    }

    private static RequestBuilder reinitialisation(String jeton, String motDePasse) {
        return post("/api/v1/auth/reinitialisation").contentType(MediaType.APPLICATION_JSON)
                .content("{\"jeton\":\"" + jeton + "\",\"nouveauMotDePasse\":\"" + motDePasse + "\"}");
    }

    /** Les e-mails envoyés à une adresse depuis le début du test, dans l'ordre. */
    private List<String> courriels(String email) {
        return mockingDetails(messagerie).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("send") && invocation.getArguments().length == 1)
                .map(invocation -> invocation.getArgument(0))
                .filter(SimpleMailMessage.class::isInstance).map(SimpleMailMessage.class::cast)
                .filter(m -> m.getTo() != null && m.getTo().length > 0 && m.getTo()[0].equalsIgnoreCase(email))
                .map(SimpleMailMessage::getText)
                .toList();
    }

    /** Attend l'e-mail qui suit les {@code avant} déjà reçus : l'envoi est asynchrone. */
    private String courrielSuivant(String email, int avant) throws InterruptedException {
        for (int attente = 0; attente < 100; attente++) {
            List<String> recus = courriels(email);
            if (recus.size() > avant) {
                return recus.get(recus.size() - 1);
            }
            Thread.sleep(50);
        }
        throw new AssertionError("aucun nouvel e-mail pour " + email);
    }

    private static String extraire(String texte, Pattern motif) {
        Matcher m = motif.matcher(texte);
        assertThat(m.find()).as("motif %s dans « %s »", motif, texte).isTrue();
        return m.group(1);
    }

    private JsonNode corps(ResultActions resultat) throws Exception {
        return corps(resultat.andReturn());
    }

    private JsonNode corps(MvcResult resultat) throws Exception {
        return json.readTree(resultat.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
