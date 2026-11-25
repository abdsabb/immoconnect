package be.immoconnect.services;

import static org.assertj.core.api.Assertions.assertThat;

import be.immoconnect.config.ConfigurationHorloge;
import be.immoconnect.dto.Creneau;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** Test unitaire des règles horaires des visites : horloge figée au mercredi 7 octobre 2026 à 12 h. */
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
    void unePlageSeDecoupeEnVisitesDUneHeureEspaceesDUneDemiHeure() {
        LocalDate jeudi = MERCREDI.plusDays(1);

        // 9 h – 12 h : deux visites ; celle de 12 h dépasserait la fin de la plage
        assertThat(grille.decouper(jeudi.atTime(9, 0), jeudi.atTime(12, 0)))
                .containsExactly(jeudi.atTime(9, 0), jeudi.atTime(10, 30));
        // 14 h – 21 h : cinq visites, la dernière se termine à la fermeture
        assertThat(grille.decouper(jeudi.atTime(14, 0), jeudi.atTime(21, 0)))
                .containsExactly(jeudi.atTime(14, 0), jeudi.atTime(15, 30), jeudi.atTime(17, 0), jeudi.atTime(18, 30), jeudi.atTime(20, 0));
        // Les créneaux partent de l'heure choisie par l'agent, pas d'une grille fixe
        assertThat(grille.decouper(jeudi.atTime(13, 30), jeudi.atTime(16, 0)))
                .containsExactly(jeudi.atTime(13, 30), jeudi.atTime(15, 0));
    }

    @Test
    void unePlageTropCourteNeDonneAucunCreneau() {
        assertThat(grille.decouper(MERCREDI.atTime(14, 0), MERCREDI.atTime(14, 30))).isEmpty();
        assertThat(grille.decouper(MERCREDI.atTime(14, 0), MERCREDI.atTime(15, 0))).containsExactly(MERCREDI.atTime(14, 0));
    }

    @Test
    void unCreneauPasseOuTropLointainNEstPasDansLesDelais() {
        assertThat(grille.dansLesDelais(MERCREDI.atTime(10, 30))).isFalse();
        assertThat(grille.dansLesDelais(MERCREDI.atTime(14, 0))).isTrue();
        assertThat(grille.dansLesDelais(MERCREDI.plusDays(GrilleCreneaux.HORIZON_JOURS).atTime(10, 30))).isTrue();
        assertThat(grille.dansLesDelais(MERCREDI.plusDays(GrilleCreneaux.HORIZON_JOURS + 1).atTime(10, 30))).isFalse();
        assertThat(grille.dansLesDelais(LocalDateTime.of(2027, 3, 3, 10, 30))).isFalse();
    }

    @Test
    void unCreneauTropProcheNEstPasDansLesDelais() {
        // Standard : deux heures. Mercredi 12 h : 14 h passe tout juste, 13 h 30 est trop proche.
        assertThat(grille.dansLesDelais(MERCREDI.atTime(14, 0))).isTrue();
        assertThat(grille.dansLesDelais(MERCREDI.atTime(13, 30))).isFalse();
        // Premium : vingt-quatre heures. Le soir même est trop proche, le lendemain soir et le samedi conviennent.
        assertThat(grille.dansLesDelais(MERCREDI.atTime(18, 30))).isFalse();
        assertThat(grille.dansLesDelais(MERCREDI.atTime(20, 0))).isFalse();
        assertThat(grille.dansLesDelais(MERCREDI.plusDays(1).atTime(18, 30))).isTrue();
        assertThat(grille.dansLesDelais(SAMEDI.atTime(10, 30))).isTrue();
        assertThat(grille.delaiMinimum(MERCREDI.atTime(14, 0))).isEqualTo(GrilleCreneaux.DELAI_STANDARD);
        assertThat(grille.delaiMinimum(SAMEDI.atTime(14, 0))).isEqualTo(GrilleCreneaux.DELAI_PREMIUM);
    }

    /** Les agents de test n'ont pas tous le même agenda : jour de repos, soirées et samedi dépendent de l'agent. */
    @Test
    void laSemaineTypeDeDemonstrationDiffereDUnAgentALAutre() {
        // Premier agent : repos le lundi, soirées du mardi et du jeudi, samedi travaillé
        assertThat(DisponibilitesDeDemonstration.semaineType(0, DayOfWeek.MONDAY)).isEmpty();
        assertThat(DisponibilitesDeDemonstration.semaineType(0, DayOfWeek.TUESDAY)).hasSize(2)
                .last().extracting(DisponibilitesDeDemonstration.Plage::fin).isEqualTo(GrilleCreneaux.FERMETURE);
        assertThat(DisponibilitesDeDemonstration.semaineType(0, DayOfWeek.WEDNESDAY)).hasSize(2)
                .last().extracting(DisponibilitesDeDemonstration.Plage::fin).isEqualTo(java.time.LocalTime.of(18, 0));
        assertThat(DisponibilitesDeDemonstration.semaineType(0, DayOfWeek.SATURDAY)).hasSize(1);
        // Deuxième agent : repos le mardi, pas de samedi ; personne ne reçoit le dimanche
        assertThat(DisponibilitesDeDemonstration.semaineType(1, DayOfWeek.TUESDAY)).isEmpty();
        assertThat(DisponibilitesDeDemonstration.semaineType(1, DayOfWeek.SATURDAY)).isEmpty();
        for (int rang = 0; rang < 8; rang++) {
            assertThat(DisponibilitesDeDemonstration.semaineType(rang, DayOfWeek.SUNDAY)).isEmpty();
        }
    }
}
