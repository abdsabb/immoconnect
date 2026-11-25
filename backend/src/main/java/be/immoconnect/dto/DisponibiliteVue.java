package be.immoconnect.dto;

import be.immoconnect.entities.Disponibilite;
import java.time.LocalDateTime;

/** Plage de disponibilité telle que l'agent la voit dans son agenda, avec le nombre de créneaux qu'elle offre. */
public record DisponibiliteVue(Integer id, LocalDateTime debut, LocalDateTime fin, int creneaux) {

    public static DisponibiliteVue depuis(Disponibilite d, int creneaux) {
        return new DisponibiliteVue(d.getId(), d.getDebut(), d.getFin(), creneaux);
    }
}
