package be.immoconnect.notification;

/**
 * Événement d'un compte utilisateur, publié par les services et traité après validation de la
 * transaction (pattern Observer). Le secret — lien ou code — ne transite qu'en mémoire.
 */
public record EvenementCompte(Type type, Integer utilisateurId, String secret) {

    public enum Type {
        /** Lien de confirmation de l'adresse e-mail d'un nouveau compte. */
        activation,
        /** Lien de choix d'un nouveau mot de passe. */
        reinitialisation,
        /** Le mot de passe vient d'être changé : information, sans secret. */
        mot_de_passe_modifie,
        /** Code du double facteur d'une connexion. */
        code_connexion
    }
}
