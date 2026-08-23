package be.immoconnect.api.auth;

/** Réponse de /auth/login et /auth/register, au format documenté au livrable 15 (§4). */
public record ReponseJeton(String jeton, String type, long expireDans, UtilisateurResume utilisateur) {

    public static ReponseJeton bearer(String jeton, long expireDans, UtilisateurResume utilisateur) {
        return new ReponseJeton(jeton, "Bearer", expireDans, utilisateur);
    }
}
