package be.immoconnect.exceptions;

/** Trop de tentatives de connexion : réponse 429 avec le délai d'attente, sans révéler si le compte existe. */
public class TropDeTentativesException extends RuntimeException {

    private final long secondesAvantReprise;

    public TropDeTentativesException(long secondesAvantReprise) {
        super("Trop de tentatives de connexion");
        this.secondesAvantReprise = Math.max(1, secondesAvantReprise);
    }

    public long getSecondesAvantReprise() {
        return secondesAvantReprise;
    }
}
