package be.immoconnect.exceptions;

/**
 * Lien ou code refusé : inconnu, expiré ou déjà utilisé. Le message reste le même dans les trois cas,
 * pour ne rien apprendre à qui essaie des valeurs au hasard.
 */
public class JetonInvalideException extends RuntimeException {

    public JetonInvalideException() {
        super("Ce lien ou ce code n'est plus valable");
    }
}
