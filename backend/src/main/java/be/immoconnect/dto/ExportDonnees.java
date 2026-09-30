package be.immoconnect.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Toutes les données personnelles d'un compte, dans un format structuré et lisible par une machine
 * (droit à la portabilité, article 20 du RGPD). Les identifiants internes n'y figurent pas : ils ne
 * concernent que le site.
 */
public record ExportDonnees(
        LocalDateTime exporteLe,
        Compte compte,
        List<Favori> favoris,
        List<Visite> visites,
        List<Paiement> paiements,
        List<Message> messages,
        List<Signalement> signalements,
        List<Historique> historique) {

    public record Compte(String prenom, String nom, String email, String telephone, String role, String langue,
                         LocalDate inscritLe, LocalDateTime cguAccepteesLe, boolean consentementCommunications,
                         boolean doubleFacteur) {
    }

    public record Favori(String bien, String ville, LocalDateTime ajouteLe) {
    }

    public record Visite(String bien, String ville, String agent, LocalDateTime dateHeure, String statut, String motif,
                         boolean premium) {
    }

    public record Paiement(String bien, LocalDateTime visite, BigDecimal montant, String statut, LocalDateTime payeLe,
                           String reference) {
    }

    public record Message(String interlocuteur, boolean envoye, String contenu, LocalDateTime envoyeLe, boolean lu) {
    }

    public record Signalement(String typeContenu, String motif, String description, String statut, LocalDateTime creeLe,
                              LocalDateTime traiteLe, String decision) {
    }

    public record Historique(String action, String entite, LocalDateTime horodatage, String ip) {
    }
}
