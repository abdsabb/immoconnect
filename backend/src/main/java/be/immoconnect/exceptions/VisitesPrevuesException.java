package be.immoconnect.exceptions;

/**
 * L'agent retire une plage de disponibilité où des visites sont déjà prévues (réponse 409) : rien n'est retiré
 * tant qu'il n'a pas confirmé qu'elles doivent être annulées (RA16).
 */
public class VisitesPrevuesException extends RuntimeException {

    private final int visites;

    public VisitesPrevuesException(int visites) {
        super(visites + " visite(s) sont prévues pendant cette période : confirmez leur annulation pour retirer la disponibilité");
        this.visites = visites;
    }

    public int getVisites() {
        return visites;
    }
}
