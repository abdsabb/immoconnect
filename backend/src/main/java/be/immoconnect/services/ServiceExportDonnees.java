package be.immoconnect.services;

import be.immoconnect.dto.ExportDonnees;
import be.immoconnect.entities.AgentImmobilier;
import be.immoconnect.entities.Membre;
import be.immoconnect.entities.Message;
import be.immoconnect.entities.RendezVous;
import be.immoconnect.entities.Utilisateur;
import be.immoconnect.exceptions.RessourceIntrouvableException;
import be.immoconnect.repositories.FavoriRepository;
import be.immoconnect.repositories.JournalAuditRepository;
import be.immoconnect.repositories.MessageRepository;
import be.immoconnect.repositories.RendezVousRepository;
import be.immoconnect.repositories.UtilisateurRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Droit à la portabilité (article 20 du RGPD, chapitre 11 du rapport) : l'utilisateur télécharge tout ce
 * que le site sait de lui, en JSON. La demande est elle-même journalisée (RA13).
 */
@Service
public class ServiceExportDonnees {

    private final UtilisateurRepository utilisateurs;
    private final FavoriRepository favoris;
    private final RendezVousRepository rendezVous;
    private final MessageRepository messages;
    private final JournalAuditRepository journal;
    private final ServiceAudit audit;
    private final Clock horloge;

    public ServiceExportDonnees(UtilisateurRepository utilisateurs, FavoriRepository favoris, RendezVousRepository rendezVous,
                                MessageRepository messages, JournalAuditRepository journal,
                                ServiceAudit audit, Clock horloge) {
        this.utilisateurs = utilisateurs;
        this.favoris = favoris;
        this.rendezVous = rendezVous;
        this.messages = messages;
        this.journal = journal;
        this.audit = audit;
        this.horloge = horloge;
    }

    @Transactional
    public ExportDonnees exporter(Integer utilisateurId, String ip) {
        Utilisateur u = utilisateurs.findById(utilisateurId)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur", utilisateurId));
        boolean membre = u instanceof Membre;
        boolean agent = u instanceof AgentImmobilier;
        String telephone = membre ? ((Membre) u).getTelephone() : agent ? ((AgentImmobilier) u).getTelephonePro() : null;

        var compte = new ExportDonnees.Compte(u.getPrenom(), u.getNom(), u.getEmail(), telephone, u.getRole(),
                u.getLangue().getCode(), u.getDateInscription(), u.getCguAccepteesLe(), u.isConsentementCommunications(),
                u.exigeDoubleFacteur());

        List<ExportDonnees.Favori> mesFavoris = membre
                ? favoris.findByIdMembreIdOrderByDateAjoutDesc(utilisateurId).stream()
                        .map(f -> new ExportDonnees.Favori(f.getBien().getTitre(), f.getBien().getVille(), f.getDateAjout())).toList()
                : List.of();

        List<RendezVous> visites = membre ? rendezVous.findByMembreIdOrderByDateHeureDesc(utilisateurId)
                : agent ? rendezVous.findByAgentIdOrderByDateHeureDesc(utilisateurId) : List.of();
        List<ExportDonnees.Visite> mesVisites = visites.stream()
                .map(r -> new ExportDonnees.Visite(r.getBien().getTitre(), r.getBien().getVille(), r.getAgent().getNomComplet(),
                        r.getDateHeure(), r.getStatut().name(), r.getMotif(), r.estPremium())).toList();
        List<ExportDonnees.Paiement> mesPaiements = membre
                ? visites.stream().filter(RendezVous::estPremium)
                        .map(r -> new ExportDonnees.Paiement(r.getBien().getTitre(), r.getDateHeure(), r.getPaiement().getMontant(),
                                r.getPaiement().getStatut().name(), r.getPaiement().getPayeLe(),
                                r.getPaiement().getStripePaymentIntentId())).toList()
                : List.of();

        List<Message> echanges = membre ? messages.findByMembreIdOrderByEnvoyeLeDescIdDesc(utilisateurId)
                : agent ? messages.findByAgentIdOrderByEnvoyeLeDescIdDesc(utilisateurId) : List.of();
        List<ExportDonnees.Message> mesMessages = echanges.stream()
                .map(m -> new ExportDonnees.Message(
                        (m.getAuteur().getId().equals(utilisateurId) ? m.getDestinataire() : m.getAuteur()).getNomComplet(),
                        m.getAuteur().getId().equals(utilisateurId), m.getContenu(), m.getEnvoyeLe(), m.isLu())).toList();

        // La demande d'export fait elle-même partie de l'historique exporté
        audit.enregistrer(u, "export_donnees", "utilisateur#" + utilisateurId, ip);
        List<ExportDonnees.Historique> historique = journal.findByUtilisateurIdOrderByHorodatageAsc(utilisateurId).stream()
                .map(j -> new ExportDonnees.Historique(j.getAction(), j.getEntite(), j.getHorodatage(), j.getIp())).toList();

        return new ExportDonnees(LocalDateTime.now(horloge), compte, mesFavoris, mesVisites, mesPaiements, mesMessages,
                historique);
    }
}
