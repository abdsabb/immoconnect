package be.immoconnect.entite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import be.immoconnect.entite.RendezVous.TransitionInterditeException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** Test unitaire du cycle de vie d'un rendez-vous (diagramme d'état-transition, règles RA1 et RA2). */
class RendezVousTest {

    private static final LocalDateTime VISITE = LocalDateTime.of(2026, 10, 8, 10, 30);
    private static final LocalDateTime AVANT = VISITE.minusDays(2);
    private static final LocalDateTime APRES = VISITE.plusHours(2);

    private static RendezVous rendezVous() {
        return new RendezVous(null, new Bien(), VISITE, null);
    }

    @Test
    void unRendezVousNaitAuStatutDemande() {
        assertThat(rendezVous().getStatut()).isEqualTo(StatutRendezVous.demande);
    }

    @Test
    void leCycleNominalVaDeDemandeAHonore() {
        RendezVous rdv = rendezVous();

        rdv.confirmer(AVANT);
        assertThat(rdv.getStatut()).isEqualTo(StatutRendezVous.confirme);

        rdv.honorer(APRES);
        assertThat(rdv.getStatut()).isEqualTo(StatutRendezVous.honore);
    }

    @Test
    void unRendezVousHonoreNePeutPlusEtreAnnule() {
        RendezVous rdv = rendezVous();
        rdv.confirmer(AVANT);
        rdv.honorer(APRES);

        assertThatThrownBy(() -> rdv.annuler(APRES)).isInstanceOf(TransitionInterditeException.class);
        assertThat(rdv.getStatut()).isEqualTo(StatutRendezVous.honore);
    }

    @Test
    void unRendezVousAnnuleNePeutPlusEtreConfirme() {
        RendezVous rdv = rendezVous();
        rdv.annuler(AVANT);

        assertThatThrownBy(() -> rdv.confirmer(AVANT)).isInstanceOf(TransitionInterditeException.class);
    }

    @Test
    void onNeConfirmeNiNAnnuleUneVisiteDontLaDateEstPassee() {
        RendezVous rdv = rendezVous();

        assertThatThrownBy(() -> rdv.confirmer(APRES)).isInstanceOf(TransitionInterditeException.class);
        assertThatThrownBy(() -> rdv.annuler(APRES)).isInstanceOf(TransitionInterditeException.class);
    }

    @Test
    void onNHonorePasUneVisiteQuiNAPasEncoreEuLieu() {
        RendezVous rdv = rendezVous();
        rdv.confirmer(AVANT);

        assertThatThrownBy(() -> rdv.honorer(AVANT)).isInstanceOf(TransitionInterditeException.class);
    }
}
