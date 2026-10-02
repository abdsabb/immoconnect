package be.immoconnect.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Mise en page HTML des e-mails : bouton, code de connexion, libellés, et texte saisi toujours échappé. */
class MiseEnPageCourrielTest {

    private static final String SITE = "https://immoconnect.test";

    @Test
    void leLienDuModeleDevientUnBoutonEtLesLibellesSontEnGras() {
        String html = MiseEnPageCourriel.html("fr", "Votre visite est confirmée", "Bonjour Léa,\n\nDate : lundi à 09:00\nAdresse : Rue Haute 1\n\nVotre espace :\n"
                + SITE + "/rendez-vous", "Voir mes visites", null, "L’équipe ImmoConnect", SITE);

        assertThat(html).contains("<html lang=\"fr\">", "<h1", "Votre visite est confirmée", "<strong>Date</strong> : lundi à 09:00",
                "<strong>Adresse</strong> : Rue Haute 1", "<a href=\"" + SITE + "/rendez-vous\"", "Voir mes visites</a>", "L’équipe ImmoConnect");
    }

    @Test
    void leCodeDeConnexionEstMisEnValeur() {
        String html = MiseEnPageCourriel.html("en", "Your login code: 482913", "Hello Emma,\n\nYour login code is: 482913\n\nIt is valid for 10 minutes.",
                null, "482913", "The ImmoConnect team", SITE);

        assertThat(html).contains("Your login code is:</p>", "letter-spacing:8px", ">482913</span>", "It is valid for 10 minutes.");
    }

    @Test
    void leTexteSaisiParUnVisiteurNEstJamaisDuHtmlNiUnLien() {
        String html = MiseEnPageCourriel.html("fr", "Contact : <b>offre</b>", "De : Robot\n\n<script>alert(1)</script>\nhttps://piege.test/\"onclick", null, null,
                "L’équipe ImmoConnect", SITE);

        assertThat(html).contains("Contact : &lt;b&gt;offre&lt;/b&gt;", "&lt;script&gt;alert(1)&lt;/script&gt;", "https://piege.test/&quot;onclick")
                .doesNotContain("<script", "href=\"https://piege.test");
    }
}
