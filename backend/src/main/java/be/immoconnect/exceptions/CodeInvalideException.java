package be.immoconnect.exceptions;

/** Code du double facteur incorrect : réponse 401, avec le nombre d'essais qui restent. */
public class CodeInvalideException extends RuntimeException {

    private final int essaisRestants;

    public CodeInvalideException(int essaisRestants) {
        super(essaisRestants > 0 ? "Code incorrect" : "Code incorrect, recommencez la connexion");
        this.essaisRestants = Math.max(0, essaisRestants);
    }

    public int getEssaisRestants() {
        return essaisRestants;
    }
}
