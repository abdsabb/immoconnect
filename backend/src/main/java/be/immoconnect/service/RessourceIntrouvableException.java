package be.immoconnect.service;

/** Ressource inexistante ou non visible publiquement (réponse 404, livrable 15 §5). */
public class RessourceIntrouvableException extends RuntimeException {

    public RessourceIntrouvableException(String ressource, Object id) {
        super(ressource + " " + id + " introuvable");
    }
}
