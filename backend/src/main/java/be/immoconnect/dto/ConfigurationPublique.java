package be.immoconnect.dto;

/**
 * Réglages que le navigateur a besoin de connaître : rien de secret.
 *
 * @param activationParCourriel un nouveau compte doit confirmer son adresse
 * @param doubleFacteur         un code par e-mail peut être demandé à la connexion
 * @param boiteDeDemonstration  adresse de la boîte où arrivent les e-mails d'un site de démonstration, sinon vide
 */
public record ConfigurationPublique(boolean activationParCourriel, boolean doubleFacteur, String boiteDeDemonstration) {
}
