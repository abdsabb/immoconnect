package be.immoconnect.api.rendezvous;

import be.immoconnect.entite.Paiement;
import be.immoconnect.entite.RendezVous;
import be.immoconnect.entite.StatutPaiement;
import be.immoconnect.entite.StatutRendezVous;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Rendez-vous tel que le voient le membre (ses visites) et l'agent (son agenda).
 * L'adresse exacte du bien, masquée dans le catalogue public, n'est communiquée au membre
 * qu'une fois la visite confirmée (livrables 10 et 16) ; l'agent, lui, la voit toujours.
 */
public record RendezVousResume(
        Integer id,
        Integer bienId,
        String bienTitre,
        String ville,
        String adresse,
        String agent,
        String membre,
        LocalDateTime dateHeure,
        StatutRendezVous statut,
        String motif,
        Creneau.Type type,
        PaiementResume paiement) {

    public record PaiementResume(BigDecimal montant, StatutPaiement statut, LocalDateTime payeLe) {

        static PaiementResume depuis(Paiement paiement) {
            return paiement == null ? null
                    : new PaiementResume(paiement.getMontant(), paiement.getStatut(), paiement.getPayeLe());
        }
    }

    public static RendezVousResume pourMembre(RendezVous rdv) {
        boolean visiteAcquise = rdv.getStatut() == StatutRendezVous.confirme || rdv.getStatut() == StatutRendezVous.honore;
        return depuis(rdv, visiteAcquise);
    }

    public static RendezVousResume pourAgent(RendezVous rdv) {
        return depuis(rdv, true);
    }

    private static RendezVousResume depuis(RendezVous rdv, boolean adresseVisible) {
        return new RendezVousResume(rdv.getId(), rdv.getBien().getId(), rdv.getBien().getTitre(), rdv.getBien().getVille(),
                adresseVisible ? rdv.getBien().getAdresse() + ", " + rdv.getBien().getCodePostal() + " " + rdv.getBien().getVille() : null,
                rdv.getAgent().getNomComplet(), rdv.getMembre().getNomComplet(), rdv.getDateHeure(), rdv.getStatut(),
                rdv.getMotif(), rdv.estPremium() ? Creneau.Type.premium : Creneau.Type.standard,
                PaiementResume.depuis(rdv.getPaiement()));
    }
}
