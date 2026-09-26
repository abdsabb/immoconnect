package be.immoconnect.service;

import static org.assertj.core.api.Assertions.assertThat;

import be.immoconnect.api.rendezvous.Creneau;
import be.immoconnect.config.ConfigurationHorloge;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** Test unitaire de la grille horaire : horloge figée au mercredi 7 octobre 2026 à 12 h. */
class GrilleCreneauxTest {

    private static final LocalDate MERCREDI = LocalDate.of(2026, 10, 7);
    private static final LocalDate SAMEDI = LocalDate.of(2026, 10, 10);

    private final Clock horloge = Clock.fixed(
            MERCREDI.atTime(12, 0).atZone(ConfigurationHorloge.FUSEAU_AGENCE).toInstant(), ConfigurationHorloge.FUSEAU_AGENCE);

    private final GrilleCreneaux grille = new GrilleCreneaux(new BigDecimal("15.00"), horloge);

    @Test
    void unCreneauDeJourneeEnSemaineEstStandardEtGratuit() {
        Creneau creneau = grille.creneau(MERCREDI.plusDays(1).atTime(10, 30));

        assertThat(creneau.type()).isEqualTo(Creneau.Type.standard);
        assertThat(creneau.prix()).isEqualByComparingTo("0");
    }

    @Test
    void laSoireeEtLeWeekEndSontPremium() {
        assertThat(grille.creneau(MERCREDI.plusDays(1).atTime(18, 30)).type()).isEqualTo(Creneau.Type.premium);
        assertThat(grille.creneau(SAMEDI.atTime(10, 30)).type()).isEqualTo(Creneau.Type.premium);
        assertThat(grille.prix(SAMEDI.atTime(14, 0))).isEqualByComparingTo("15.00");
    }

    @Test
    void uneHeureHorsGrilleNEstPasReservable() {
        assertThat(grille.estReservable(MERCREDI.plusDays(1).atTime(10, 45))).isFalse();
        assertThat(grille.estReservable(SAMEDI.atTime(20, 0))).isFalse();
    }

    @Test
    void unCreneauPasseOuTropLointainNEstPasReservable() {
        assertThat(grille.estReservable(MERCREDI.atTime(10, 30))).isFalse();
        assertThat(grille.estReservable(MERCREDI.atTime(14, 0))).isTrue();
        assertThat(grille.estReservable(LocalDateTime.of(2027, 3, 3, 10, 30))).isFalse();
    }

    @Test
    void laPeriodeNeContientQueLesCreneauxEncoreAVenir() {
        // Mercredi à 12 h : il reste 14 h, 15 h 30, 17 h, 18 h 30 et 20 h ; jeudi compte ses 7 créneaux.
        assertThat(grille.entre(MERCREDI, MERCREDI.plusDays(1))).hasSize(12)
                .first().isEqualTo(MERCREDI.atTime(14, 0));
        assertThat(grille.entre(SAMEDI, SAMEDI)).hasSize(3);
    }
}
