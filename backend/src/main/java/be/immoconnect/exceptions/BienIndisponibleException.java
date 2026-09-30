package be.immoconnect.exceptions;

/** Le bien n'accepte plus ni rendez-vous ni favori : sous option, vendu ou loué (réponse 409, règle RA5, cas d'erreur E3). */
public class BienIndisponibleException extends IllegalStateException {

    public BienIndisponibleException(Integer bienId) {
        super("Le bien " + bienId + " n'est plus disponible");
    }
}
