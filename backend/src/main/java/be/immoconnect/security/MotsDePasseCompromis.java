package be.immoconnect.security;

/**
 * Dit si un mot de passe figure dans une fuite connue (pattern Adapter) : l'application ignore si la
 * réponse vient d'un service en ligne ou d'une liste embarquée.
 */
public interface MotsDePasseCompromis {

    boolean contient(String motDePasse);
}
