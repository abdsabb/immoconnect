package be.immoconnect.dto;

/**
 * Réponse de /auth/login quand un second facteur est exigé (202) : le code est parti par e-mail,
 * le défi doit être renvoyé avec lui à /auth/login/code.
 */
public record ReponseDefi(boolean doubleFacteur, String defi, long expireDans) {

    public static ReponseDefi de(String defi, long expireDans) {
        return new ReponseDefi(true, defi, expireDans);
    }
}
