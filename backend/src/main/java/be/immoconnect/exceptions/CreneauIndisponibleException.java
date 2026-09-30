package be.immoconnect.exceptions;

/** Le créneau demandé est déjà occupé (réponse 409, scénario alternatif A2 de l'analyse). */
public class CreneauIndisponibleException extends IllegalStateException {

    public CreneauIndisponibleException(String message) {
        super(message);
    }
}
