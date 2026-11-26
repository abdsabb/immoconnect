package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Les visites des données de test ont été générées avant que la règle des créneaux ne soit fixée : 42 visites payées
 * tombaient en journée de semaine (créneau standard, donc gratuit) et 21 visites non payées tombaient le week-end
 * (créneau premium, donc payant). Cette migration les replace sur un créneau conforme à la règle RA7 :
 * <ul>
 *   <li>une visite payée passe en soirée, 18 h 30 ou 20 h, le même jour si possible ;</li>
 *   <li>une visite non payée du week-end passe au premier jour ouvrable suivant, à la même heure.</li>
 * </ul>
 * Chaque visite est déplacée une à une, vers une heure où ni son agent ni son membre n'ont d'autre visite : ce
 * contrôle ligne par ligne est la raison d'une migration écrite en Java plutôt qu'en SQL. Seule la date change,
 * jamais le statut ni le paiement.
 */
public class V17__Visites_de_test_conformes_aux_creneaux extends BaseJavaMigration {

    private static final List<LocalTime> SOIREE = List.of(LocalTime.of(18, 30), LocalTime.of(20, 0));
    private static final int JOURS_DE_RECHERCHE = 30;
    /** Les dates voyagent en texte : ni le fuseau du serveur ni celui de la connexion ne peuvent les décaler. */
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String PAYEES_EN_JOURNEE = """
            SELECT r.id, r.agent_id, r.membre_id, DATE_FORMAT(r.date_heure, '%Y-%m-%d %H:%i:%s') AS date_heure FROM rendez_vous r
            JOIN paiement p ON p.rendez_vous_id = r.id
            WHERE WEEKDAY(r.date_heure) < 5 AND HOUR(r.date_heure) < 18
            ORDER BY r.date_heure, r.id
            """;
    private static final String NON_PAYEES_DU_WEEK_END = """
            SELECT r.id, r.agent_id, r.membre_id, DATE_FORMAT(r.date_heure, '%Y-%m-%d %H:%i:%s') AS date_heure FROM rendez_vous r
            LEFT JOIN paiement p ON p.rendez_vous_id = r.id
            WHERE p.id IS NULL AND WEEKDAY(r.date_heure) >= 5
            ORDER BY r.date_heure, r.id
            """;

    private record Visite(int id, int agentId, int membreId, LocalDateTime dateHeure) {
    }

    @Override
    public void migrate(Context contexte) throws SQLException {
        Connection connexion = contexte.getConnection();
        for (Visite visite : lire(connexion, PAYEES_EN_JOURNEE)) {
            deplacer(connexion, visite, soireeLibre(connexion, visite));
        }
        for (Visite visite : lire(connexion, NON_PAYEES_DU_WEEK_END)) {
            deplacer(connexion, visite, jourOuvrableLibre(connexion, visite));
        }
    }

    /** Première soirée de semaine où l'agent et le membre sont libres, à partir du jour de la visite. */
    private static LocalDateTime soireeLibre(Connection connexion, Visite visite) throws SQLException {
        LocalDate jour = visite.dateHeure().toLocalDate();
        for (int i = 0; i < JOURS_DE_RECHERCHE; i++, jour = jour.plusDays(1)) {
            if (estWeekEnd(jour)) {
                continue;
            }
            for (LocalTime heure : SOIREE) {
                if (estLibre(connexion, visite, jour.atTime(heure))) {
                    return jour.atTime(heure);
                }
            }
        }
        throw new SQLException("Aucune soirée libre pour la visite " + visite.id());
    }

    /** Premier jour ouvrable après la visite où l'agent et le membre sont libres à la même heure. */
    private static LocalDateTime jourOuvrableLibre(Connection connexion, Visite visite) throws SQLException {
        LocalDate jour = visite.dateHeure().toLocalDate().plusDays(1);
        LocalTime heure = visite.dateHeure().toLocalTime();
        for (int i = 0; i < JOURS_DE_RECHERCHE; i++, jour = jour.plusDays(1)) {
            if (!estWeekEnd(jour) && estLibre(connexion, visite, jour.atTime(heure))) {
                return jour.atTime(heure);
            }
        }
        throw new SQLException("Aucun jour ouvrable libre pour la visite " + visite.id());
    }

    private static boolean estLibre(Connection connexion, Visite visite, LocalDateTime dateHeure) throws SQLException {
        String sql = "SELECT COUNT(*) FROM rendez_vous WHERE id <> ? AND date_heure = ? AND (agent_id = ? OR membre_id = ?)";
        try (PreparedStatement requete = connexion.prepareStatement(sql)) {
            requete.setInt(1, visite.id());
            requete.setString(2, FORMAT.format(dateHeure));
            requete.setInt(3, visite.agentId());
            requete.setInt(4, visite.membreId());
            try (ResultSet resultat = requete.executeQuery()) {
                resultat.next();
                return resultat.getInt(1) == 0;
            }
        }
    }

    private static void deplacer(Connection connexion, Visite visite, LocalDateTime dateHeure) throws SQLException {
        try (PreparedStatement requete = connexion.prepareStatement("UPDATE rendez_vous SET date_heure = ? WHERE id = ?")) {
            requete.setString(1, FORMAT.format(dateHeure));
            requete.setInt(2, visite.id());
            requete.executeUpdate();
        }
    }

    private static List<Visite> lire(Connection connexion, String sql) throws SQLException {
        List<Visite> visites = new ArrayList<>();
        try (PreparedStatement requete = connexion.prepareStatement(sql); ResultSet resultat = requete.executeQuery()) {
            while (resultat.next()) {
                visites.add(new Visite(resultat.getInt("id"), resultat.getInt("agent_id"), resultat.getInt("membre_id"),
                        LocalDateTime.parse(resultat.getString("date_heure"), FORMAT)));
            }
        }
        return visites;
    }

    private static boolean estWeekEnd(LocalDate jour) {
        return jour.getDayOfWeek() == DayOfWeek.SATURDAY || jour.getDayOfWeek() == DayOfWeek.SUNDAY;
    }
}
