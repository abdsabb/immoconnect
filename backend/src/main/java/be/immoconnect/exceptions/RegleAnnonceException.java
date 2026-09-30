package be.immoconnect.exceptions;

/** L'état de l'annonce interdit l'opération demandée (réponse 409) : publier sans photo, archiver avec des visites prévues… */
public class RegleAnnonceException extends IllegalStateException {

    public RegleAnnonceException(String message) {
        super(message);
    }
}
