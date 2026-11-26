package be.immoconnect.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import be.immoconnect.CourrielRecu;
import be.immoconnect.TestcontainersConfiguration;
import be.immoconnect.config.ConfigurationHorloge;
import be.immoconnect.paiement.PasserelleSimulee;
import jakarta.mail.internet.MimeMessage;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.hamcrest.Matchers;
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

/**
 * Cas « Gérer ses disponibilités » (AG7) : l'agent ouvre et retire ses plages, le membre ne se voit proposer que
 * les créneaux de ces plages (RA15), et le retrait d'une plage annule les visites prévues en prévenant les
 * membres (RA16). Sur un vrai MySQL 8.4.
 * <p>
 * L'agent du test (108) reçoit du lundi au vendredi sauf le mercredi, jamais le week-end : les plages ouvertes
 * ici le sont un mercredi, un samedi ou un dimanche, à plus de vingt jours, loin des créneaux des autres tests.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class DisponibiliteControleurTest {

    private static final int AGENT = 108;
    private static final String AUTRE_AGENT = "sarah.dubois@mail.be";
    private static final String MEMBRE = "alice.benali@mail.be";
    private static final int ATTENTE_MS = 5000;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private JavaMailSender messagerie;

    @Test
    void lAgendaDUnAgentNeProposePasTousLesJours() throws Exception {
        List<DayOfWeek> jours = corps(mvc.perform(get("/api/v1/biens/" + bien() + "/creneaux").header("Authorization", "Bearer " + connecter(MEMBRE)))
                .andExpect(status().isOk())).valueStream()
                .map(c -> LocalDateTime.parse(c.get("dateHeure").asString()).getDayOfWeek()).distinct().toList();

        // Sur deux semaines : des créneaux, mais ni le mercredi (jour de repos de l'agent) ni le week-end
        assertThat(jours).isNotEmpty().doesNotContain(DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
        mvc.perform(get("/api/v1/disponibilites").header("Authorization", "Bearer " + agent()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.not(Matchers.empty())))
                .andExpect(jsonPath("$[*].creneaux", Matchers.everyItem(Matchers.greaterThan(0))));
    }

    @Test
    void lesDisponibilitesSontReserveesAuxAgents() throws Exception {
        mvc.perform(get("/api/v1/disponibilites")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/disponibilites").header("Authorization", "Bearer " + connecter(MEMBRE)))
                .andExpect(status().isForbidden());
        ouvrir(connecter(MEMBRE), prochain(DayOfWeek.SUNDAY, 30).atTime(10, 0), prochain(DayOfWeek.SUNDAY, 30).atTime(13, 0), null)
                .andExpect(status().isForbidden());
    }

    @Test
    void lAgentOuvreUnePlageEtLeMembreVoitSesCreneaux() throws Exception {
        String agent = agent();
        String membre = connecter(MEMBRE);
        LocalDate dimanche = prochain(DayOfWeek.SUNDAY, 30);
        assertThat(creneaux(membre, dimanche)).isEmpty();

        // 10 h – 13 h : deux visites d'une heure, à 10 h et 11 h 30 ; un dimanche, elles sont premium
        int id = corps(ouvrir(agent, dimanche.atTime(10, 0), dimanche.atTime(13, 0), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].creneaux").value(2))).get(0).get("id").asInt();
        assertThat(creneaux(membre, dimanche)).containsExactly(dimanche.atTime(10, 0).toString() + ":00", dimanche.atTime(11, 30).toString() + ":00");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_audit WHERE action = 'ouverture_disponibilite' AND entite = ?",
                Integer.class, "disponibilite#" + id)).isEqualTo(1);

        // Deux plages du même agent ne se chevauchent pas
        ouvrir(agent, dimanche.atTime(12, 0), dimanche.atTime(14, 0), null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.champs.debut").isNotEmpty());
        // RA15 : hors de la plage, rien ne se réserve
        mvc.perform(post("/api/v1/paiements/intent").header("Authorization", "Bearer " + membre).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bienId\":" + bien() + ",\"dateHeure\":\"" + dimanche.atTime(14, 0) + ":00\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.champs.dateHeure").isNotEmpty());

        // Contrôle de propriété : un autre agent ne retire pas cette plage
        mvc.perform(delete("/api/v1/disponibilites/" + id).header("Authorization", "Bearer " + connecter(AUTRE_AGENT)))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/disponibilites/" + id).header("Authorization", "Bearer " + agent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plagesRetirees").value(1))
                .andExpect(jsonPath("$.visitesAnnulees").value(0));
        assertThat(creneaux(membre, dimanche)).isEmpty();
        mvc.perform(delete("/api/v1/disponibilites/" + id).header("Authorization", "Bearer " + agent))
                .andExpect(status().isNotFound());
    }

    @Test
    void unePlageInvalideEstRefusee() throws Exception {
        String agent = agent();
        LocalDate jour = prochain(DayOfWeek.WEDNESDAY, 27);

        ouvrir(agent, jour.atTime(18, 0), jour.plusDays(1).atTime(9, 0), null)
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.champs.fin").isNotEmpty());
        ouvrir(agent, jour.atTime(10, 0), jour.atTime(10, 30), null)
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.champs.fin").isNotEmpty());
        ouvrir(agent, jour.atTime(10, 15), jour.atTime(12, 0), null)
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.champs.debut").isNotEmpty());
        ouvrir(agent, jour.atTime(7, 0), jour.atTime(9, 0), null)
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.champs.debut").isNotEmpty());
        ouvrir(agent, jour.atTime(20, 0), jour.atTime(21, 30), null)
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.champs.debut").isNotEmpty());
        ouvrir(agent, aujourdHui().minusDays(1).atTime(10, 0), aujourdHui().minusDays(1).atTime(12, 0), null)
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.champs.debut").isNotEmpty());
        ouvrir(agent, aujourdHui().plusDays(70).atTime(10, 0), aujourdHui().plusDays(70).atTime(12, 0), null)
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.champs.debut").isNotEmpty());
        ouvrir(agent, jour.atTime(10, 0), jour.atTime(12, 0), 9)
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.champs.semaines").isNotEmpty());
        assertThat(plages(agent, jour, jour)).isEmpty();
    }

    @Test
    void unePlageSeRepeteChaqueSemaine() throws Exception {
        String agent = agent();
        LocalDate dimanche = prochain(DayOfWeek.SUNDAY, 37);

        ouvrir(agent, dimanche.atTime(14, 0), dimanche.atTime(16, 0), 3)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[1].debut").value(dimanche.plusWeeks(1).atTime(14, 0) + ":00"))
                .andExpect(jsonPath("$[2].fin").value(dimanche.plusWeeks(2).atTime(16, 0) + ":00"));

        // La répétition ne dépasse pas l'horizon de réservation : rien n'est ouvert, pas même la première semaine
        LocalDate dernier = prochain(DayOfWeek.SUNDAY, 54);
        ouvrir(agent, dernier.atTime(9, 0), dernier.atTime(10, 0), 2)
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.champs.semaines").isNotEmpty());
        assertThat(plages(agent, dernier, dernier).valueStream().map(p -> p.get("debut").asString()))
                .doesNotContain(dernier.atTime(9, 0) + ":00");
    }

    /** L'agent tombe malade : il retire ses plages, les visites sont annulées, les membres prévenus et remboursés. */
    @Test
    void retirerUnePlageAnnuleLesVisitesPrevuesEtPrevientLesMembres() throws Exception {
        String agent = agent();
        String membre = corps(mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Peeters\",\"prenom\":\"Lina\",\"email\":\"lina.peeters@test.immoconnect.be\",\"motDePasse\":\"Visite-Bxl-2026\",\"cguAcceptees\":true}"))
                .andExpect(status().isCreated())).get("jeton").asString();
        LocalDate mercredi = prochain(DayOfWeek.WEDNESDAY, 20);
        LocalDate samedi = prochain(DayOfWeek.SATURDAY, 20);
        int plage = corps(ouvrir(agent, mercredi.atTime(9, 0), mercredi.atTime(12, 0), null).andExpect(status().isCreated())).get(0).get("id").asInt();
        ouvrir(agent, samedi.atTime(10, 0), samedi.atTime(13, 0), null).andExpect(status().isCreated());

        // Une visite standard le mercredi, une visite premium payée le samedi
        String creneau = mercredi.atTime(9, 0) + ":00";
        int visite = corps(reserver(membre, creneau, null).andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("demande"))).get("id").asInt();
        String creneauPremium = samedi.atTime(10, 0) + ":00";
        JsonNode intention = corps(mvc.perform(post("/api/v1/paiements/intent").header("Authorization", "Bearer " + membre)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"bienId\":" + bien() + ",\"dateHeure\":\"" + creneauPremium + "\"}"))
                .andExpect(status().isCreated()));
        mvc.perform(post("/api/v1/paiements/simulation/" + intention.get("paymentIntentId").asString() + "/payer")
                        .header("Authorization", "Bearer " + membre).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientSecret\":\"" + intention.get("clientSecret").asString() + "\",\"numeroCarte\":\"" + PasserelleSimulee.CARTE_ACCEPTEE + "\"}"))
                .andExpect(status().isNoContent());
        int visitePayee = corps(reserver(membre, creneauPremium, intention.get("paymentIntentId").asString())
                .andExpect(status().isCreated()).andExpect(jsonPath("$.statut").value("confirme"))).get("id").asInt();

        // RA16 : sans accord explicite, rien n'est retiré et la visite reste prévue
        mvc.perform(delete("/api/v1/disponibilites/" + plage).header("Authorization", "Bearer " + agent))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://www.immoconnect.be/erreurs/visites-prevues"))
                .andExpect(jsonPath("$.visites").value(1));
        assertThat(statut(visite)).isEqualTo("demande");
        assertThat(plages(agent, mercredi, mercredi)).hasSize(1);

        // L'agent confirme : la visite est annulée et le membre reçoit un e-mail
        reset(messagerie);
        mvc.perform(delete("/api/v1/disponibilites/" + plage + "?annulerVisites=true").header("Authorization", "Bearer " + agent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plagesRetirees").value(1))
                .andExpect(jsonPath("$.visitesAnnulees").value(1));
        assertThat(statut(visite)).isEqualTo("annule");
        assertThat(plages(agent, mercredi, mercredi)).isEmpty();
        ArgumentCaptor<MimeMessage> envois = ArgumentCaptor.forClass(MimeMessage.class);
        verify(messagerie, timeout(ATTENTE_MS).atLeastOnce()).send(envois.capture());
        assertThat(envois.getAllValues().stream().map(CourrielRecu::de))
                .anySatisfy(courriel -> {
                    assertThat(courriel.destinataires()).containsExactly("lina.peeters@test.immoconnect.be");
                    assertThat(courriel.sujet()).startsWith("[ImmoConnect] Votre visite est annulée");
                    assertThat(courriel.texte()).contains("a dû annuler votre visite", "choisir un autre créneau");
                });
        // Le créneau n'existe plus : il ne se réserve plus
        reserver(membre, creneau, null).andExpect(status().isUnprocessableContent());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_audit WHERE action = 'retrait_disponibilite' AND entite = ?",
                Integer.class, "disponibilite#" + plage)).isEqualTo(1);

        // « Indisponible du … au … » : même règle, et le créneau payé est remboursé (RA8)
        mvc.perform(delete("/api/v1/disponibilites?du=" + samedi + "&au=" + samedi).header("Authorization", "Bearer " + agent))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.visites").value(1));
        mvc.perform(delete("/api/v1/disponibilites?du=" + samedi + "&au=" + samedi + "&annulerVisites=true").header("Authorization", "Bearer " + agent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plagesRetirees").value(1))
                .andExpect(jsonPath("$.visitesAnnulees").value(1));
        assertThat(statut(visitePayee)).isEqualTo("annule");
        assertThat(jdbc.queryForObject("SELECT statut FROM paiement WHERE rendez_vous_id = ?", String.class, visitePayee)).isEqualTo("rembourse");
    }

    @Test
    void seDeclarerIndisponibleRetireToutesLesPlagesDeLaPeriode() throws Exception {
        String agent = agent();
        String membre = connecter(MEMBRE);
        LocalDate du = aujourdHui().plusDays(45);
        LocalDate au = du.plusDays(2);
        int ouvertes = plages(agent, du, au).size();
        assertThat(ouvertes).isPositive();

        mvc.perform(delete("/api/v1/disponibilites?du=" + au + "&au=" + du).header("Authorization", "Bearer " + agent))
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.champs.au").isNotEmpty());
        mvc.perform(delete("/api/v1/disponibilites?du=" + du + "&au=" + au).header("Authorization", "Bearer " + agent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plagesRetirees").value(ouvertes))
                .andExpect(jsonPath("$.visitesAnnulees").value(0));

        assertThat(plages(agent, du, au)).isEmpty();
        for (LocalDate jour = du; !jour.isAfter(au); jour = jour.plusDays(1)) {
            assertThat(creneaux(membre, jour)).isEmpty();
        }
    }

    private ResultActions ouvrir(String jeton, LocalDateTime debut, LocalDateTime fin, Integer semaines) throws Exception {
        String corps = "{\"debut\":\"" + debut + ":00\",\"fin\":\"" + fin + ":00\"" + (semaines == null ? "" : ",\"semaines\":" + semaines) + "}";
        return mvc.perform(post("/api/v1/disponibilites").header("Authorization", "Bearer " + jeton)
                .contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    private JsonNode plages(String jeton, LocalDate du, LocalDate au) throws Exception {
        return corps(mvc.perform(get("/api/v1/disponibilites?du=" + du + "&au=" + au).header("Authorization", "Bearer " + jeton))
                .andExpect(status().isOk()));
    }

    private List<String> creneaux(String jeton, LocalDate jour) throws Exception {
        return corps(mvc.perform(get("/api/v1/biens/" + bien() + "/creneaux?du=" + jour + "&au=" + jour).header("Authorization", "Bearer " + jeton))
                .andExpect(status().isOk())).valueStream().map(c -> c.get("dateHeure").asString()).toList();
    }

    private ResultActions reserver(String jeton, String dateHeure, String paymentIntentId) throws Exception {
        String corps = "{\"bienId\":" + bien() + ",\"dateHeure\":\"" + dateHeure + "\""
                + (paymentIntentId == null ? "" : ",\"paymentIntentId\":\"" + paymentIntentId + "\"") + "}";
        return mvc.perform(post("/api/v1/rendez-vous").header("Authorization", "Bearer " + jeton)
                .contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    private String statut(int rendezVous) {
        return jdbc.queryForObject("SELECT statut FROM rendez_vous WHERE id = ?", String.class, rendezVous);
    }

    /** Un bien disponible de l'agent du test. */
    private int bien() {
        return jdbc.queryForObject("SELECT MIN(id) FROM bien WHERE agent_id = ? AND statut = 'disponible'", Integer.class, AGENT);
    }

    private String agent() throws Exception {
        return connecter(jdbc.queryForObject("SELECT email FROM utilisateur WHERE id = ?", String.class, AGENT));
    }

    private static LocalDate aujourdHui() {
        return LocalDate.now(ConfigurationHorloge.FUSEAU_AGENCE);
    }

    /** Le premier jour de la semaine demandé, à au moins {@code dansJours} jours d'aujourd'hui. */
    private static LocalDate prochain(DayOfWeek jour, int dansJours) {
        LocalDate date = aujourdHui().plusDays(dansJours);
        while (date.getDayOfWeek() != jour) {
            date = date.plusDays(1);
        }
        return date;
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
