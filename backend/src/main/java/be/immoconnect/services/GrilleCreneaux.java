package be.immoconnect.services;

import be.immoconnect.dto.Creneau;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Grille horaire des visites de l'agence.
 * <ul>
 *   <li>créneaux <b>standard</b>, gratuits : du lundi au vendredi, en journée ;</li>
 *   <li>créneaux <b>premium</b>, payants : en soirée (à partir de 18 h) et le week-end.</li>
 * </ul>
 * La grille est la seule source de vérité : une date qui n'y figure pas n'est pas réservable,
 * et c'est elle — jamais le client — qui décide si un créneau est payant et à quel prix.
 */
@Component
public class GrilleCreneaux {

    /** On ne réserve pas plus de deux mois à l'avance. */
    public static final int HORIZON_JOURS = 60;

    private static final LocalTime DEBUT_SOIREE = LocalTime.of(18, 0);

    private static final List<LocalTime> SEMAINE = List.of(
            LocalTime.of(9, 0), LocalTime.of(10, 30), LocalTime.of(14, 0), LocalTime.of(15, 30), LocalTime.of(17, 0),
            LocalTime.of(18, 30), LocalTime.of(20, 0));

    private static final List<LocalTime> WEEK_END = List.of(
            LocalTime.of(10, 30), LocalTime.of(14, 0), LocalTime.of(15, 30));

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

    /** Un créneau est réservable s'il figure dans la grille, s'il est futur et dans l'horizon de réservation. */
    public boolean estReservable(LocalDateTime dateHeure) {
        LocalDateTime maintenant = LocalDateTime.now(horloge);
        return heures(dateHeure.toLocalDate()).contains(dateHeure.toLocalTime())
                && dateHeure.isAfter(maintenant)
                && !dateHeure.toLocalDate().isAfter(maintenant.toLocalDate().plusDays(HORIZON_JOURS));
    }

    /** Tous les créneaux réservables de la période, bornes incluses, dans l'ordre chronologique. */
    public List<LocalDateTime> entre(LocalDate du, LocalDate au) {
        List<LocalDateTime> creneaux = new ArrayList<>();
        for (LocalDate jour = du; !jour.isAfter(au); jour = jour.plusDays(1)) {
            for (LocalTime heure : heures(jour)) {
                LocalDateTime dateHeure = jour.atTime(heure);
                if (estReservable(dateHeure)) {
                    creneaux.add(dateHeure);
                }
            }
        }
        return creneaux;
    }

    public LocalDate aujourdHui() {
        return LocalDate.now(horloge);
    }

    private static List<LocalTime> heures(LocalDate jour) {
        return estWeekEnd(jour) ? WEEK_END : SEMAINE;
    }

    private static boolean estWeekEnd(LocalDate jour) {
        return jour.getDayOfWeek() == DayOfWeek.SATURDAY || jour.getDayOfWeek() == DayOfWeek.SUNDAY;
    }
}
