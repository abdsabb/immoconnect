package be.immoconnect.entite;

/**
 * Nature de l'offre d'un bien (valeurs = colonne ENUM bien.type_offre). Elle décide du sens du prix :
 * un prix de vente, ou un loyer mensuel.
 */
public enum TypeOffre {
    vente, location;

    /** Un bien à vendre ne finit pas « loué », un bien à louer ne finit pas « vendu ». */
    public boolean accepte(StatutBien statut) {
        return switch (this) {
            case vente -> statut != StatutBien.loue;
            case location -> statut != StatutBien.vendu;
        };
    }
}
