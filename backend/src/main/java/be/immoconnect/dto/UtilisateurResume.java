package be.immoconnect.dto;

import be.immoconnect.entities.Utilisateur;

/** Identité de l'utilisateur connecté exposée par l'API (jamais le mot de passe). */
public record UtilisateurResume(Integer id, String prenom, String nom, String email, String role, String langue,
                                boolean doubleFacteur, boolean consentementCommunications) {

    public static UtilisateurResume depuis(Utilisateur u) {
        return new UtilisateurResume(u.getId(), u.getPrenom(), u.getNom(), u.getEmail(), u.getRole(), u.getLangue().getCode(),
                u.exigeDoubleFacteur(), u.isConsentementCommunications());
    }
}
