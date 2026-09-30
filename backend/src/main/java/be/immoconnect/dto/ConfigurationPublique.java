package be.immoconnect.dto;

/**
 * Réglages que le navigateur a besoin de connaître : rien de secret.
 *
 * @param activationParCourriel un nouveau compte doit confirmer son adresse
 * @param doubleFacteur         un code par e-mail peut être demandé à la connexion
 * @param boiteDeDemonstration  adresse de la boîte où arrivent les e-mails d'un site de démonstration, sinon vide
 * @param site                  identité de l'agence et langues actives, réglées par l'administrateur (cas A5)
 * @param mesure                mesure d'audience Matomo auto-hébergée, ou null si elle n'est pas configurée
 */
public record ConfigurationPublique(boolean activationParCourriel, boolean doubleFacteur, String boiteDeDemonstration,
                                    SiteInfos site, Mesure mesure) {

    /** Adresse de Matomo sur ce même domaine et numéro du site à y alimenter. */
    public record Mesure(String url, int siteId) {
    }
}
