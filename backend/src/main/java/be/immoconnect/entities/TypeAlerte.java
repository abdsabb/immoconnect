package be.immoconnect.entities;

/** Ce qui a déclenché une alerte de sécurité (valeurs = colonne ENUM alerte_securite.type). */
public enum TypeAlerte {
    /** De nombreux échecs de connexion depuis une même adresse IP. */
    rafale_echecs,
    /** Une même adresse IP essaie de nombreux comptes différents. */
    enumeration,
    /** Une clé API révoquée a été présentée (RA12). */
    cle_revoquee
}
