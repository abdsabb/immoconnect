package be.immoconnect.exceptions;

/** Les identifiants sont bons, mais l'adresse e-mail du compte n'a pas encore été confirmée. */
public class CompteNonActiveException extends RuntimeException {

    public CompteNonActiveException() {
        super("Confirmez votre adresse e-mail avant de vous connecter");
    }
}
