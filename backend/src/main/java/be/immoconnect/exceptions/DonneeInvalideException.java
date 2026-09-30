package be.immoconnect.exceptions;

/**
 * Une donnée est bien formée mais refusée par une règle métier (réponse 422) : le champ fautif est
 * nommé pour que l'interface affiche le message au bon endroit, comme pour une validation de formulaire.
 */
public class DonneeInvalideException extends RuntimeException {

    private final String champ;

    public DonneeInvalideException(String champ, String message) {
        super(message);
        this.champ = champ;
    }

    public String getChamp() {
        return champ;
    }
}
