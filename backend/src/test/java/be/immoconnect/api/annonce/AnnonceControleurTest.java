package be.immoconnect.api.annonce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import be.immoconnect.TestcontainersConfiguration;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Back-office de l'agent (AG1, AG2, AG3, AG6) : cycle de vie d'une annonce, photos, règle RA6 et
 * contrôle de propriété, sur un vrai MySQL 8.4.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AnnonceControleurTest {

    private static final String AGENT = "karim.haddad@mail.be";
    private static final String AUTRE_AGENT = "nadia.elamrani@mail.be";
    private static final String MEMBRE = "alice.benali@mail.be";

    private static final String ANNONCE = """
            {"categorieId": 2, "titre": "Appartement 2 chambres — Schaerbeek", "description": "Lumineux, proche du parc Josaphat.",
             "prix": 289000, "superficie": 92.5, "nbChambres": 2, "adresse": "Avenue Louis Bertrand 40",
             "ville": "Schaerbeek", "codePostal": "1030", "latitude": 50.8642, "longitude": 4.3789%s}
            """;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void uneAnnonceNaitHorsLigneEtSePublieAvecSaPremierePhoto() throws Exception {
        String agent = connecter(AGENT);

        int id = corps(creer(agent)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("archive"))
                .andExpect(jsonPath("$.adresse").value("Avenue Louis Bertrand 40"))
                .andExpect(jsonPath("$.photos", Matchers.empty()))).get("id").asInt();
        mvc.perform(get("/api/v1/biens/" + id)).andExpect(status().isNotFound());

        // RA6 : pas de publication sans photo
        modifier(agent, id, "disponible")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", Matchers.containsString("RA6")));

        // Un PNG est accepté, mais c'est un JPEG qui est enregistré et servi
        String url = corps(televerser(agent, id, image("png", 1800, 1200), "photo.png", "Séjour")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.photos", Matchers.hasSize(1)))
                .andExpect(jsonPath("$.photos[0].ordre").value(1))
                .andExpect(jsonPath("$.photos[0].legende").value("Séjour"))
                .andExpect(jsonPath("$.photos[0].url", Matchers.matchesPattern("/storage/biens/" + id + "/[0-9a-f-]{36}\\.jpg"))))
                .get("photos").get(0).get("url").asString();
        byte[] servi = mvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andReturn().getResponse().getContentAsByteArray();
        BufferedImage relue = ImageIO.read(new java.io.ByteArrayInputStream(servi));
        assertThat(relue.getWidth()).isEqualTo(1600);
        assertThat(relue.getHeight()).isLessThanOrEqualTo(1200);

        modifier(agent, id, "disponible")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("disponible"));
        mvc.perform(get("/api/v1/biens/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photos[0].url").value(url))
                .andExpect(jsonPath("$.agent.nomComplet").value("Karim Haddad"))
                .andExpect(jsonPath("$.adresse").doesNotExist());
        // Création, photo, publication : trois traces. La publication refusée n'en laisse pas, sa transaction est annulée.
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_audit WHERE entite = ? AND action IN ('creation_bien', 'modification_bien', 'ajout_photo')",
                Integer.class, "bien#" + id)).isEqualTo(3);
    }

    @Test
    void unFichierQuiNEstPasUneImageEstRefuse() throws Exception {
        String agent = connecter(AGENT);
        int id = corps(creer(agent).andExpect(status().isCreated())).get("id").asInt();

        // Le nom et le type annoncé mentent : seul le contenu fait foi
        byte[] script = "<?php system($_GET['c']); ?>".getBytes(StandardCharsets.UTF_8);
        televerser(agent, id, script, "vacances.jpg", null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.champs.fichier").value("le fichier n'est pas une image"));
        televerser(agent, id, image("png", 120, 90), "vignette.png", null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.champs.fichier", Matchers.containsString("trop petite")));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM photo WHERE bien_id = ?", Integer.class, id)).isZero();
    }

    @Test
    void laCouvertureSeChoisitEtLaDernierePhotoDUneAnnonceEnLigneNeSeSupprimePas() throws Exception {
        String agent = connecter(AGENT);
        int id = corps(creer(agent).andExpect(status().isCreated())).get("id").asInt();
        televerser(agent, id, image("jpg", 800, 600), "facade.jpg", "Façade").andExpect(status().isCreated());
        JsonNode galerie = corps(televerser(agent, id, image("jpg", 800, 600), "cuisine.jpg", "Cuisine").andExpect(status().isCreated()));
        int facade = galerie.get("photos").get(0).get("id").asInt();
        int cuisine = galerie.get("photos").get(1).get("id").asInt();

        mvc.perform(put("/api/v1/biens/" + id + "/photos/" + cuisine + "/couverture").header("Authorization", "Bearer " + agent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photos[0].legende").value("Cuisine"))
                .andExpect(jsonPath("$.photos[0].ordre").value(1))
                .andExpect(jsonPath("$.photos[1].legende").value("Façade"))
                .andExpect(jsonPath("$.photos[1].ordre").value(2));

        modifier(agent, id, "disponible").andExpect(status().isOk());
        mvc.perform(delete("/api/v1/biens/" + id + "/photos/" + cuisine).header("Authorization", "Bearer " + agent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photos", Matchers.hasSize(1)))
                .andExpect(jsonPath("$.photos[0].legende").value("Façade"))
                .andExpect(jsonPath("$.photos[0].ordre").value(1));
        mvc.perform(delete("/api/v1/biens/" + id + "/photos/" + facade).header("Authorization", "Bearer " + agent))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", Matchers.containsString("RA6")));
    }

    @Test
    void unBienNeSArchivePasTantQueDesVisitesSontPrevues() throws Exception {
        String agent = connecter(AGENT);
        String membre = connecter(MEMBRE);
        int id = corps(creer(agent).andExpect(status().isCreated())).get("id").asInt();
        televerser(agent, id, image("jpg", 800, 600), "facade.jpg", null).andExpect(status().isCreated());
        modifier(agent, id, "disponible").andExpect(status().isOk());

        String creneau = corps(mvc.perform(get("/api/v1/biens/" + id + "/creneaux").header("Authorization", "Bearer " + membre))
                .andExpect(status().isOk())).valueStream()
                .filter(c -> "standard".equals(c.get("type").asString())).findFirst().orElseThrow().get("dateHeure").asString();
        int visite = corps(mvc.perform(post("/api/v1/rendez-vous").header("Authorization", "Bearer " + membre)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bienId\":" + id + ",\"dateHeure\":\"" + creneau + "\"}"))
                .andExpect(status().isCreated())).get("id").asInt();

        // Tableau de bord : la demande apparaît sur l'annonce
        mvc.perform(get("/api/v1/agents/moi/biens").header("Authorization", "Bearer " + agent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")].indicateurs.demandesEnAttente", Matchers.contains(1)));

        mvc.perform(delete("/api/v1/biens/" + id).header("Authorization", "Bearer " + agent))
                .andExpect(status().isConflict());
        modifier(agent, id, "vendu").andExpect(status().isConflict());

        mvc.perform(patch("/api/v1/rendez-vous/" + visite + "/annuler").header("Authorization", "Bearer " + agent))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/v1/biens/" + id).header("Authorization", "Bearer " + agent))
                .andExpect(status().isNoContent());

        // Archivée, l'annonce quitte le site public mais reste dans le back-office
        mvc.perform(get("/api/v1/biens/" + id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/agents/moi/biens/" + id).header("Authorization", "Bearer " + agent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("archive"));
    }

    @Test
    void unAgentNeToucheNiNeVoitLesAnnoncesDUnCollegue() throws Exception {
        int id = corps(creer(connecter(AGENT)).andExpect(status().isCreated())).get("id").asInt();
        String collegue = connecter(AUTRE_AGENT);

        modifier(collegue, id, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://www.immoconnect.be/erreurs/interdit"));
        mvc.perform(get("/api/v1/agents/moi/biens/" + id).header("Authorization", "Bearer " + collegue))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/biens/" + id).header("Authorization", "Bearer " + collegue))
                .andExpect(status().isForbidden());
        televerser(collegue, id, image("jpg", 800, 600), "intrus.jpg", null).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/agents/moi/biens").header("Authorization", "Bearer " + collegue))
                .andExpect(jsonPath("$[*].id", Matchers.not(Matchers.hasItem(id))));
    }

    @Test
    void publierEstReserveAuxAgents() throws Exception {
        mvc.perform(post("/api/v1/biens").contentType(MediaType.APPLICATION_JSON).content(ANNONCE.formatted("")))
                .andExpect(status().isUnauthorized());
        creer(connecter(MEMBRE)).andExpect(status().isForbidden());
        creer(connecter("david.moreau@mail.be")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/agents/moi/biens").header("Authorization", "Bearer " + connecter(MEMBRE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void uneAnnonceInvalideEstRefuseeChampParChamp() throws Exception {
        mvc.perform(post("/api/v1/biens").header("Authorization", "Bearer " + connecter(AGENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categorieId": 2, "titre": " ", "description": "Test", "prix": -5, "superficie": 0,
                                 "nbChambres": 25, "adresse": "Rue Test 1", "ville": "Bruxelles", "codePostal": "10000",
                                 "latitude": 95, "longitude": 4.35}
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.champs.titre").isNotEmpty())
                .andExpect(jsonPath("$.champs.prix").isNotEmpty())
                .andExpect(jsonPath("$.champs.superficie").isNotEmpty())
                .andExpect(jsonPath("$.champs.nbChambres").isNotEmpty())
                .andExpect(jsonPath("$.champs.codePostal").isNotEmpty())
                .andExpect(jsonPath("$.champs.latitude").isNotEmpty());
    }

    @Test
    void lesCategoriesSontPubliques() throws Exception {
        mvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(Matchers.greaterThanOrEqualTo(10))))
                .andExpect(jsonPath("$[0].nom").value("Appartement"));
    }

    private ResultActions creer(String jeton) throws Exception {
        return mvc.perform(post("/api/v1/biens").header("Authorization", "Bearer " + jeton)
                .contentType(MediaType.APPLICATION_JSON).content(ANNONCE.formatted("")));
    }

    private ResultActions modifier(String jeton, int id, String statut) throws Exception {
        String champStatut = statut == null ? "" : ", \"statut\": \"" + statut + "\"";
        return mvc.perform(put("/api/v1/biens/" + id).header("Authorization", "Bearer " + jeton)
                .contentType(MediaType.APPLICATION_JSON).content(ANNONCE.formatted(champStatut)));
    }

    private ResultActions televerser(String jeton, int id, byte[] contenu, String nom, String legende) throws Exception {
        var requete = multipart("/api/v1/biens/" + id + "/photos")
                .file(new MockMultipartFile("fichier", nom, "image/jpeg", contenu));
        if (legende != null) {
            requete.param("legende", legende);
        }
        return mvc.perform(requete.header("Authorization", "Bearer " + jeton));
    }

    private static byte[] image(String format, int largeur, int hauteur) throws Exception {
        BufferedImage image = new BufferedImage(largeur, hauteur, BufferedImage.TYPE_INT_RGB);
        Graphics2D dessin = image.createGraphics();
        dessin.setColor(new Color(0x2A9D8F));
        dessin.fillRect(0, 0, largeur, hauteur);
        dessin.setColor(new Color(0xE76F51));
        dessin.fillOval(largeur / 4, hauteur / 4, largeur / 2, hauteur / 2);
        dessin.dispose();
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        ImageIO.write(image, format, sortie);
        return sortie.toByteArray();
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
