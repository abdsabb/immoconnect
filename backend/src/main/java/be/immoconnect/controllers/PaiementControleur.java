package be.immoconnect.controllers;

import be.immoconnect.dto.ConfigurationPaiementPublique;
import be.immoconnect.dto.ReponseIntention;
import be.immoconnect.dto.RequetePaiement;
import be.immoconnect.dto.RequetePaiementSimule;
import be.immoconnect.exceptions.RessourceIntrouvableException;
import be.immoconnect.paiement.PasserellePaiement;
import be.immoconnect.paiement.PasserelleSimulee;
import be.immoconnect.services.GrilleCreneaux;
import be.immoconnect.services.ServiceRendezVous;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Paiement des créneaux premium (livrable 15, §6.6) : intention de paiement et webhook Stripe. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Paiements", description = "Paiement Stripe des créneaux de visite premium")
public class PaiementControleur {

    private final ServiceRendezVous service;
    private final PasserellePaiement passerelle;
    private final GrilleCreneaux grille;

    public PaiementControleur(ServiceRendezVous service, PasserellePaiement passerelle, GrilleCreneaux grille) {
        this.service = service;
        this.passerelle = passerelle;
        this.grille = grille;
    }

    @GetMapping("/paiements/config")
    @Operation(summary = "Réglages de paiement du navigateur", description = "Mode (stripe ou simulation), clé publiable et prix du créneau premium.")
    public ConfigurationPaiementPublique configuration() {
        return new ConfigurationPaiementPublique(passerelle.mode(), passerelle.clePublique(), grille.prixPremium());
    }

    @PostMapping("/paiements/intent")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "jwt")
    @Operation(summary = "Préparer le paiement d'un créneau premium (membre)",
            description = "Crée l'intention de paiement Stripe du créneau et renvoie le secret qui permet au navigateur "
                    + "de saisir la carte. Le montant est fixé par le serveur. Créneau déjà pris : 409.")
    public ReponseIntention preparer(@AuthenticationPrincipal Jwt jeton, @Valid @RequestBody RequetePaiement requete) {
        return service.preparerPaiement(Integer.valueOf(jeton.getSubject()), requete);
    }

    /**
     * Notification de Stripe. L'endpoint est public mais chaque requête est authentifiée par sa
     * signature : sans signature valide, aucun statut de paiement n'est accepté (livrable 16).
     * Le corps est lu brut, la signature portant sur les octets reçus.
     */
    @PostMapping("/webhooks/stripe")
    @Operation(summary = "Webhook Stripe (signature vérifiée)", description = "Événements payment_intent.succeeded, "
            + "payment_intent.payment_failed et charge.refunded.")
    public ResponseEntity<Void> webhook(@RequestBody String charge,
                                        @RequestHeader(name = "Stripe-Signature", required = false) String signature) {
        passerelle.lireEvenement(charge, signature).ifPresent(service::traiterEvenement);
        return ResponseEntity.ok().build();
    }

    /** Formulaire de carte du mode simulation ; inexistant (404) dès que Stripe est configuré. */
    @PostMapping("/paiements/simulation/{id}/payer")
    @SecurityRequirement(name = "jwt")
    @Operation(summary = "Régler une intention en mode simulation (membre)",
            description = "Développement et tests uniquement. Carte 4242 4242 4242 4242 : acceptée (204) ; autre carte : refusée (402).")
    public ResponseEntity<Void> payerEnSimulation(@PathVariable String id, @Valid @RequestBody RequetePaiementSimule requete) {
        if (!(passerelle instanceof PasserelleSimulee simulee)) {
            throw new RessourceIntrouvableException("Paiement simulé", id);
        }
        boolean accepte = simulee.payer(id, requete.clientSecret(), requete.numeroCarte());
        return ResponseEntity.status(accepte ? HttpStatus.NO_CONTENT : HttpStatus.PAYMENT_REQUIRED).build();
    }
}
