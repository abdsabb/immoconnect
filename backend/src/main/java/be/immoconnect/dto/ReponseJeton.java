package be.immoconnect.dto;

/**
 * Réponse de /auth/login, /auth/register et /auth/refresh (livrable 15, §4). Quand l'adresse e-mail
 * doit d'abord être confirmée, il n'y a pas encore de jeton : {@code activationRequise} le dit.
 */
public record ReponseJeton(String jeton, String type, long expireDans, UtilisateurResume utilisateur, boolean activationRequise) {

    public static ReponseJeton bearer(String jeton, long expireDans, UtilisateurResume utilisateur) {
        return new ReponseJeton(jeton, "Bearer", expireDans, utilisateur, false);
    }

    public static ReponseJeton enAttenteActivation(UtilisateurResume utilisateur) {
        return new ReponseJeton(null, null, 0, utilisateur, true);
    }
}
