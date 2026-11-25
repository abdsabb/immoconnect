package be.immoconnect.services;

import be.immoconnect.entities.AgentImmobilier;
import be.immoconnect.entities.Disponibilite;
import be.immoconnect.repositories.AgentImmobilierRepository;
import be.immoconnect.repositories.DisponibiliteRepository;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Disponibilités des agents de test. Une plage se définit par rapport à la date du jour : une migration Flyway,
 * jouée une seule fois, laisserait un agenda vide quelques semaines plus tard. Au démarrage, l'agenda de chaque
 * agent de test est donc prolongé jusqu'à l'horizon de réservation, à partir de la dernière plage qu'il possède —
 * une plage retirée par l'agent n'est jamais recréée.
 * <p>
 * Chaque agent reçoit sa propre semaine type : un jour de repos, deux soirées, un samedi sur deux agents, jamais
 * le dimanche. Les agendas diffèrent ainsi d'un agent à l'autre, comme dans une vraie agence.
 * Un agent créé par un administrateur n'est pas concerné : il ouvre ses plages lui-même.
 */
@Component
@ConditionalOnProperty(name = "immoconnect.demonstration.disponibilites", havingValue = "true")
public class DisponibilitesDeDemonstration implements ApplicationRunner {

    private static final Logger journal = LoggerFactory.getLogger(DisponibilitesDeDemonstration.class);

    /** Agents des données de test (migration V2). */
    static final int PREMIER_AGENT = 101;
    static final int DERNIER_AGENT = 108;

    private static final LocalTime MATIN = LocalTime.of(9, 0);
    private static final LocalTime MIDI = LocalTime.of(12, 0);
    private static final LocalTime APRES_MIDI = LocalTime.of(14, 0);
    private static final LocalTime FIN_DE_JOURNEE = LocalTime.of(18, 0);
    private static final LocalTime FIN_DE_SOIREE = LocalTime.of(21, 0);
    private static final LocalTime SAMEDI_DEBUT = LocalTime.of(10, 0);
    private static final LocalTime SAMEDI_FIN = LocalTime.of(16, 0);

    private final AgentImmobilierRepository agents;
    private final DisponibiliteRepository disponibilites;
    private final Clock horloge;

    public DisponibilitesDeDemonstration(AgentImmobilierRepository agents, DisponibiliteRepository disponibilites, Clock horloge) {
        this.agents = agents;
        this.disponibilites = disponibilites;
        this.horloge = horloge;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        LocalDate aujourdHui = LocalDate.now(horloge);
        LocalDate horizon = aujourdHui.plusDays(GrilleCreneaux.HORIZON_JOURS);
        LocalDateTime maintenant = LocalDateTime.now(horloge);
        int ouvertes = 0;
        for (int id = PREMIER_AGENT; id <= DERNIER_AGENT; id++) {
            AgentImmobilier agent = agents.findById(id).orElse(null);
            if (agent == null) {
                continue;
            }
            LocalDate premierJour = disponibilites.dernierDebut(id)
                    .map(dernier -> dernier.toLocalDate().plusDays(1))
                    .filter(jour -> jour.isAfter(aujourdHui))
                    .orElse(aujourdHui);
            List<Disponibilite> plages = new ArrayList<>();
            for (LocalDate jour = premierJour; !jour.isAfter(horizon); jour = jour.plusDays(1)) {
                for (Plage plage : semaineType(id - PREMIER_AGENT, jour.getDayOfWeek())) {
                    plages.add(new Disponibilite(agent, jour.atTime(plage.debut()), jour.atTime(plage.fin()), maintenant));
                }
            }
            disponibilites.saveAll(plages);
            ouvertes += plages.size();
        }
        if (ouvertes > 0) {
            journal.info("Disponibilités de démonstration : {} plages ouvertes jusqu'au {}", ouvertes, horizon);
        }
    }

    record Plage(LocalTime debut, LocalTime fin) {
    }

    /** Plages de l'agent de rang donné pour un jour de la semaine. */
    static List<Plage> semaineType(int rang, DayOfWeek jour) {
        if (jour == DayOfWeek.SUNDAY) {
            return List.of();
        }
        if (jour == DayOfWeek.SATURDAY) {
            return rang % 2 == 0 ? List.of(new Plage(SAMEDI_DEBUT, SAMEDI_FIN)) : List.of();
        }
        int numero = jour.getValue() - 1;              // lundi = 0 … vendredi = 4
        if (numero == rang % 5) {
            return List.of();                          // jour de repos de l'agent
        }
        boolean soiree = numero == (rang + 1) % 5 || numero == (rang + 3) % 5;
        return List.of(new Plage(MATIN, MIDI), new Plage(APRES_MIDI, soiree ? FIN_DE_SOIREE : FIN_DE_JOURNEE));
    }
}
