package be.immoconnect.notification;

/**
 * Changement d'état d'un rendez-vous, publié par le service métier (pattern Observer, analyse UML §3.7).
 * Le service ignore qui l'écoute : ajouter un canal de notification ne modifie pas la réservation.
 */
public record EvenementRendezVous(Integer rendezVousId, Type type) {

    public enum Type {
        /** Créneau standard réservé : l'agent doit répondre à la demande. */
        demande,
        /** Créneau premium payé : la visite est confirmée d'office. */
        confirme_par_paiement,
        confirme_par_agent,
        annule_par_membre,
        annule_par_agent
    }
}
