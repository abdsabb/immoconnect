package be.immoconnect.services;

import be.immoconnect.dto.Creneau;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Règles horaires des visites de l'agence.
 * <p>
 * Les créneaux ne sont pas fixés d'avance : chaque agent ouvre ses plages de disponibilité, et la grille
 * découpe une plage en créneaux — une visite dure une heure, suivie d'une demi-heure de battement pour le
 * trajet de l'agent. Une plage de 9 h à 12 h donne ainsi deux créneaux : 9 h et 10 h 30.
 * <ul>
 *   <li>créneaux <b>standard</b>, gratuits : du lundi au vendredi, en journée ;</li>
 *   <li>créneaux <b>premium</b>, payants : en soirée (à partir de 18 h) et le week-end.</li>
 * </ul>
 * C'est la grille — jamais le client — qui décide si un créneau est payant et à quel prix.
 * <p>
 * Un créneau se réserve avec un délai minimum : deux heures pour un créneau standard (l'agent doit
 * encore pouvoir le confirmer ou le refuser), vingt-quatre heures pour un créneau premium, qui est
 * payé et confirmé d'office — ce délai est aussi celui de l'annulation remboursée (RA14) : tout
 * créneau premium est donc encore annulable gratuitement au moment où il est réservé.
 */
@Component
public class GrilleCreneaux {

    /** On ne réserve pas plus de deux mois à l'avance. */
    public static final int HORIZON_JOURS = 60;
    /** Délai minimum avant un créneau standard. */
    public static final Duration DELAI_STANDARD = Duration.ofHours(2);
    /** Délai minimum avant un créneau premium : payé et confirmé d'office, il doit rester annulable sans frais. */
    public static final Duration DELAI_PREMIUM = Duration.ofHours(24);
    /** Durée d'une visite. */
    public static final Duration DUREE_VISITE = Duration.ofHours(1);
    /** Écart entre deux créneaux d'une même plage : la visite, puis le trajet de l'agent. */
    public static final Duration PAS = Duration.ofMinutes(90);
    /** L'agence ne fait pas visiter avant 8 h ni après 21 h. */
    public static final LocalTime OUVERTURE = LocalTime.of(8, 0);
    public static final LocalTime FERMETURE = LocalTime.of(21, 0);

    private static final LocalTime DEBUT_SOIREE = LocalTime.of(18, 0);

    private final BigDecimal prixPremium;
    private final Clock horloge;

    public GrilleCreneaux(@Value("${immoconnect.creneau-premium.prix}") BigDecimal prixPremium, Clock horloge) {
        this.prixPremium = prixPremium;
        this.horloge = horloge;
    }

    public BigDecimal prixPremium() {
        return prixPremium;
    }

    public boolean estPremium(LocalDateTime dateHeure) {
        return estWeekEnd(dateHeure.toLocalDate()) || !dateHeure.toLocalTime().isBefore(DEBUT_SOIREE);
    }

    /** Prix du créneau : 0 pour un créneau standard (RA7 : aucun paiement). */
    public BigDecimal prix(LocalDateTime dateHeure) {
        return estPremium(dateHeure) ? prixPremium : BigDecimal.ZERO;
    }

    public Creneau creneau(LocalDateTime dateHeure) {
        return new Creneau(dateHeure, estPremium(dateHeure) ? Creneau.Type.premium : Creneau.Type.standard, prix(dateHeure));
    }

    /** Délai minimum de réservation du créneau, selon son type. */
    public Duration delaiMinimum(LocalDateTime dateHeure) {
        return estPremium(dateHeure) ? DELAI_PREMIUM : DELAI_STANDARD;
    }

    /**
     * Créneaux d'une plage de disponibilité : le premier à l'ouverture de la plage, puis toutes les 90 minutes,
     * tant que la visite se termine avant la fin de la plage.
     */
    public List<LocalDateTime> decouper(LocalDateTime debut, LocalDateTime fin) {
        List<LocalDateTime> creneaux = new ArrayList<>();
        for (LocalDateTime creneau = debut; !creneau.plus(DUREE_VISITE).isAfter(fin); creneau = creneau.plus(PAS)) {
            creneaux.add(creneau);
        }
        return creneaux;
    }

    /** Un créneau se réserve s'il respecte le délai minimum de son type et s'il est dans l'horizon de réservation. */
    public boolean dansLesDelais(LocalDateTime dateHeure) {
        LocalDateTime maintenant = LocalDateTime.now(horloge);
        return !dateHeure.isBefore(maintenant.plus(delaiMinimum(dateHeure)))
                && !dateHeure.toLocalDate().isAfter(maintenant.toLocalDate().plusDays(HORIZON_JOURS));
    }

    public LocalDate aujourdHui() {
        return LocalDate.now(horloge);
    }

    private static boolean estWeekEnd(LocalDate jour) {
        return jour.getDayOfWeek() == DayOfWeek.SATURDAY || jour.getDayOfWeek() == DayOfWeek.SUNDAY;
    }
}
