package be.immoconnect.service;

import be.immoconnect.depot.AdministrateurRepository;
import be.immoconnect.entite.Administrateur;
import org.springframework.stereotype.Component;

/**
 * Niveaux d'accès du back-office (colonne administrateur.niveau_acces) : le rôle « admin » ouvre la
 * porte du back-office, le niveau dit ce que l'on peut y faire.
 * <ul>
 *   <li>1 — éditeur : blog, catégories de biens, traductions ;</li>
 *   <li>2 — gestionnaire : en plus, comptes des membres et des agents, journal d'audit, statistiques, clés API ;</li>
 *   <li>3 — super-administrateur : en plus, comptes des administrateurs.</li>
 * </ul>
 */
@Component
public class AccesAdministrateur {

    public static final int EDITEUR = 1;
    public static final int GESTIONNAIRE = 2;
    public static final int SUPER_ADMINISTRATEUR = 3;

    private final AdministrateurRepository administrateurs;

    public AccesAdministrateur(AdministrateurRepository administrateurs) {
        this.administrateurs = administrateurs;
    }

    /** @return l'administrateur connecté, s'il a au moins le niveau demandé (sinon 403) */
    public Administrateur exiger(Integer administrateurId, int niveau) {
        Administrateur administrateur = administrateurs.findById(administrateurId)
                .orElseThrow(() -> new OperationInterditeException("Le back-office est réservé aux administrateurs"));
        if (administrateur.getNiveauAcces() < niveau) {
            throw new OperationInterditeException("Votre niveau d'accès ne permet pas cette opération");
        }
        return administrateur;
    }
}
