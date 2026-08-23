package be.immoconnect.api.traduction;

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

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class TraductionControleurTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void chaqueLangueDuSiteDisposeDeSonDictionnaire() throws Exception {
        for (String code : new String[] {"fr", "nl", "en"}) {
            mvc.perform(get("/api/v1/traductions/" + code))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$['accueil.titre']", Matchers.not(Matchers.emptyString())));
        }
    }

    @Test
    void uneLangueInconnueRenvoie404() throws Exception {
        mvc.perform(get("/api/v1/traductions/de"))
                .andExpect(status().isNotFound());
    }
}
