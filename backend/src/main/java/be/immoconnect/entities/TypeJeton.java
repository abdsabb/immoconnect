package be.immoconnect.entities;

/** Usage d'un jeton à usage unique (valeurs = colonne ENUM jeton.type). */
public enum TypeJeton {
    /** Prolonge la session : placé dans un cookie que les scripts ne peuvent pas lire. */
    rafraichissement,
    /** Confirme l'adresse e-mail d'un nouveau compte. */
    activation,
    /** Autorise le choix d'un nouveau mot de passe. */
    reinitialisation,
    /** Second facteur : le défi d'une connexion, associé au code envoyé par e-mail. */
    double_facteur
}
