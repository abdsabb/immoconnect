package be.immoconnect.entite;

/** Statuts d'un bien (énumération StatutBien de l'analyse ; valeurs = colonne ENUM bien.statut). */
public enum StatutBien {
    disponible, sous_option, vendu, loue, archive;

    /** Un bien n'est visible dans la recherche et ne prend de rendez-vous que s'il est disponible (RA5). */
    public boolean estDisponible() {
        return this == disponible;
    }
}
