package be.immoconnect.api.admin;

import be.immoconnect.entite.Administrateur;
import be.immoconnect.entite.AgentImmobilier;
import be.immoconnect.entite.Utilisateur;
import java.time.LocalDate;

/** Compte vu par l'administrateur (cas A1). Le mot de passe, même haché, n'est jamais exposé. */
public record CompteResume(Integer id, String prenom, String nom, String email, String role, String langue,
                           boolean actif, LocalDate dateInscription, String matricule, Integer niveauAcces) {

    public static CompteResume depuis(Utilisateur u) {
        return new CompteResume(u.getId(), u.getPrenom(), u.getNom(), u.getEmail(), u.getRole(), u.getLangue().getCode(),
                u.isActif(), u.getDateInscription(),
                u instanceof AgentImmobilier agent ? agent.getMatricule() : null,
                u instanceof Administrateur administrateur ? administrateur.getNiveauAcces() : null);
    }
}
