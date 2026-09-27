package be.immoconnect.api.paiement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import be.immoconnect.TestcontainersConfiguration;
import be.immoconnect.paiement.EvenementPaiement;
import be.immoconnect.paiement.PasserellePaiement;
import be.immoconnect.paiement.PasserelleSimulee;
import be.immoconnect.service.ServiceRendezVous;
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
 * Scénario nominal « créneau premium avec paiement », paiement refusé (E1), remboursement à
 * l'annulation (RA8) et notification du prestataire. Stripe est remplacé par le prestataire simulé
 * (pattern Adapter) : les tests ne dépendent ni du réseau ni d'un compte Stripe.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PaiementControleurTest {

    /** Bien disponible de l'agent Anke Vermeulen (103). */
    private static final int BIEN = 5;
    private static final String MEMBRE = "alice.benali@mail.be";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasserellePaiement passerelle;

    @Autowired
    private ServiceRendezVous service;

    @Test
    void laConfigurationPubliqueAnnonceLeModeEtLePrix() throws Exception {
        mvc.perform(get("/api/v1/paiements/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("simulation"))
                .andExpect(jsonPath("$.prixCreneauPremium").value(15.0));
    }

    @Test
    void leScenarioNominalCreeUnRendezVousConfirmeEtSonPaiement() throws Exception {
        String membre = connecter(MEMBRE);
        String creneau = premierCreneau(membre, "premium");
        JsonNode intention = preparer(membre, creneau);
        assertThat(intention.get("montant").asDouble()).isEqualTo(15.0);
        assertThat(intention.get("devise").asString()).isEqualTo("eur");
        String id = intention.get("paymentIntentId").asString();

        // Tant que la carte n'est pas débitée, le créneau ne se réserve pas.
        reserver(membre, creneau, id)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.champs.paymentIntentId").value("le paiement n'a pas abouti"));

        payer(membre, intention, PasserelleSimulee.CARTE_ACCEPTEE).andExpect(status().isNoContent());
        int rendezVous = corps(reserver(membre, creneau, id)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("confirme"))
                .andExpect(jsonPath("$.type").value("premium"))
                .andExpect(jsonPath("$.adresse").isNotEmpty())
                .andExpect(jsonPath("$.paiement.statut").value("reussi"))
                .andExpect(jsonPath("$.paiement.montant").value(15.0))).get("id").asInt();
        assertThat(jdbc.queryForObject("SELECT statut FROM paiement WHERE stripe_payment_intent_id = ?", String.class, id))
                .isEqualTo("reussi");

        // Idempotence : renvoyer la même demande, ou recevoir la notification du prestataire, ne crée rien de plus.
        reserver(membre, creneau, id).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(rendezVous));
        service.traiterEvenement(new EvenementPaiement(EvenementPaiement.Type.paiement_reussi, id));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM paiement WHERE stripe_payment_intent_id = ?", Integer.class, id))
                .isEqualTo(1);

        // RA8 : l'annulation d'une visite payée rembourse le membre.
        mvc.perform(patch("/api/v1/rendez-vous/" + rendezVous + "/annuler").header("Authorization", "Bearer " + membre))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("annule"))
                .andExpect(jsonPath("$.paiement.statut").value("rembourse"));
        assertThat(((PasserelleSimulee) passerelle).estRemboursee(id)).isTrue();
    }

    /** E1 : une carte refusée ne crée ni rendez-vous ni paiement ; le membre réessaie avec une autre carte. */
    @Test
    void unPaiementRefuseNeCreeAucunRendezVous() throws Exception {
        String membre = connecter(MEMBRE);
        String creneau = premierCreneau(membre, "premium");
        JsonNode intention = preparer(membre, creneau);
        String id = intention.get("paymentIntentId").asString();

        payer(membre, intention, PasserelleSimulee.CARTE_REFUSEE).andExpect(status().isPaymentRequired());
        reserver(membre, creneau, id).andExpect(status().isUnprocessableContent());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM paiement WHERE stripe_payment_intent_id = ?", Integer.class, id))
                .isZero();

        payer(membre, intention, PasserelleSimulee.CARTE_ACCEPTEE).andExpect(status().isNoContent());
        reserver(membre, creneau, id).andExpect(status().isCreated());
    }

    /** Le membre paie puis ferme son navigateur : la notification du prestataire crée la visite à sa place. */
    @Test
    void laNotificationDePaiementCreeLeRendezVousManquant() throws Exception {
        String membre = connecter(MEMBRE);
        String creneau = premierCreneau(membre, "premium");
        JsonNode intention = preparer(membre, creneau);
        String id = intention.get("paymentIntentId").asString();
        payer(membre, intention, PasserelleSimulee.CARTE_ACCEPTEE).andExpect(status().isNoContent());

        service.traiterEvenement(new EvenementPaiement(EvenementPaiement.Type.paiement_reussi, id));

        assertThat(jdbc.queryForObject("SELECT r.statut FROM rendez_vous r JOIN paiement p ON p.rendez_vous_id = r.id "
                + "WHERE p.stripe_payment_intent_id = ?", String.class, id)).isEqualTo("confirme");
    }

    /** A2 après paiement : le créneau a été pris entre-temps, le membre est remboursé. */
    @Test
    void unCreneauPrisPendantLePaiementEstRembourse() throws Exception {
        String membre = connecter(MEMBRE);
        String creneau = premierCreneau(membre, "premium");
        JsonNode intention = preparer(membre, creneau);
        String id = intention.get("paymentIntentId").asString();

        String rival = connecter(emailDuMembre(4));
        JsonNode intentionRivale = preparer(rival, creneau);
        payer(rival, intentionRivale, PasserelleSimulee.CARTE_ACCEPTEE).andExpect(status().isNoContent());
        reserver(rival, creneau, intentionRivale.get("paymentIntentId").asString()).andExpect(status().isCreated());

        payer(membre, intention, PasserelleSimulee.CARTE_ACCEPTEE).andExpect(status().isNoContent());
        reserver(membre, creneau, id)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://www.immoconnect.be/erreurs/creneau-indisponible"));
        assertThat(((PasserelleSimulee) passerelle).estRemboursee(id)).isTrue();
    }

    @Test
    void unPaiementNeSertNiAUnAutreMembreNiAUnAutreCreneau() throws Exception {
        String membre = connecter(MEMBRE);
        String creneau = premierCreneau(membre, "premium");
        JsonNode intention = preparer(membre, creneau);
        String id = intention.get("paymentIntentId").asString();
        payer(membre, intention, PasserelleSimulee.CARTE_ACCEPTEE).andExpect(status().isNoContent());

        reserver(connecter(emailDuMembre(5)), creneau, id)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.champs.paymentIntentId").value("paiement inconnu"));
        String autreCreneau = creneaux(membre).valueStream()
                .filter(c -> "premium".equals(c.get("type").asString()) && !creneau.equals(c.get("dateHeure").asString()))
                .findFirst().orElseThrow().get("dateHeure").asString();
        reserver(membre, autreCreneau, id)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.champs.paymentIntentId").value("ce paiement ne correspond pas à ce créneau"));
    }

    @Test
    void unCreneauStandardNeSePaiePas() throws Exception {
        String membre = connecter(MEMBRE);

        mvc.perform(post("/api/v1/paiements/intent").header("Authorization", "Bearer " + membre)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bienId\":" + BIEN + ",\"dateHeure\":\"" + premierCreneau(membre, "standard") + "\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.champs.dateHeure").isNotEmpty());
    }

    @Test
    void unWebhookSansSignatureValideEstRefuse() throws Exception {
        mvc.perform(post("/api/v1/webhooks/stripe").contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "t=1,v1=faux")
                        .content("{\"type\":\"payment_intent.succeeded\",\"data\":{\"object\":{\"id\":\"pi_faux\"}}}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void lePaiementEstReserveAuxMembres() throws Exception {
        mvc.perform(post("/api/v1/paiements/intent").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/paiements/intent").header("Authorization", "Bearer " + connecter("sarah.dubois@mail.be"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    private JsonNode preparer(String jeton, String dateHeure) throws Exception {
        return corps(mvc.perform(post("/api/v1/paiements/intent").header("Authorization", "Bearer " + jeton)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bienId\":" + BIEN + ",\"dateHeure\":\"" + dateHeure + "\",\"motif\":\"Visite après le travail\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientSecret").isNotEmpty()));
    }

    private ResultActions payer(String jeton, JsonNode intention, String carte) throws Exception {
        return mvc.perform(post("/api/v1/paiements/simulation/" + intention.get("paymentIntentId").asString() + "/payer")
                .header("Authorization", "Bearer " + jeton).contentType(MediaType.APPLICATION_JSON)
                .content("{\"clientSecret\":\"" + intention.get("clientSecret").asString() + "\",\"numeroCarte\":\"" + carte + "\"}"));
    }

    private ResultActions reserver(String jeton, String dateHeure, String paymentIntentId) throws Exception {
        return mvc.perform(post("/api/v1/rendez-vous").header("Authorization", "Bearer " + jeton)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bienId\":" + BIEN + ",\"dateHeure\":\"" + dateHeure + "\",\"paymentIntentId\":\"" + paymentIntentId + "\"}"));
    }

    private String premierCreneau(String jeton, String type) throws Exception {
        return creneaux(jeton).valueStream()
                .filter(c -> type.equals(c.get("type").asString()))
                .findFirst().orElseThrow().get("dateHeure").asString();
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
