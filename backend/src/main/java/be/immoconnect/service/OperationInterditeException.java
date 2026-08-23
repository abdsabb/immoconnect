package be.immoconnect.service;

/** L'utilisateur est authentifié mais n'a pas le droit d'effectuer cette opération (réponse 403). */
public class OperationInterditeException extends RuntimeException {

    public OperationInterditeException(String message) {
        super(message);
    }
}
