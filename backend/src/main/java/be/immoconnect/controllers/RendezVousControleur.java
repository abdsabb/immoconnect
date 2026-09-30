package be.immoconnect.controllers;

import static be.immoconnect.controllers.RequeteHttp.adresseIp;

import be.immoconnect.dto.Creneau;
import be.immoconnect.dto.RendezVousResume;
import be.immoconnect.dto.RequeteRendezVous;
import be.immoconnect.security.ConfigurationJwt;
import be.immoconnect.services.ServiceRendezVous;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rendez-vous de visite (livrable 15, §6.5). Les rôles sont contrôlés par la configuration de
 * sécurité ; la propriété de chaque rendez-vous l'est par le service.
 */
@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "jwt")
@Tag(name = "Rendez-vous", description = "Créneaux de visite, réservation et cycle de vie d'un rendez-vous")
public class RendezVousControleur {

    private final ServiceRendezVous service;

    public RendezVousControleur(ServiceRendezVous service) {
        this.service = service;
    }

    @GetMapping("/biens/{id}/creneaux")
    @Operation(summary = "Créneaux de visite encore libres (membre)",
            description = "Créneaux standard (gratuits) et premium (soirée et week-end, payants) libres dans l'agenda de "
                    + "l'agent du bien. Période par défaut : 14 jours à partir d'aujourd'hui ; 31 jours au plus.")
    public List<Creneau> creneaux(
            @PathVariable Integer id,
            @Parameter(description = "Premier jour (AAAA-MM-JJ)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @Parameter(description = "Dernier jour inclus (AAAA-MM-JJ)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au) {
        return service.creneauxLibres(id, du, au);
    }

    @GetMapping("/rendez-vous")
    @Operation(summary = "Mes rendez-vous (membre) ou mon agenda (agent)")
    public List<RendezVousResume> lister(@AuthenticationPrincipal Jwt jeton) {
        return service.lister(identifiant(jeton), jeton.getClaimAsString(ConfigurationJwt.CLAIM_ROLE));
    }

    @PostMapping("/rendez-vous")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Demander un rendez-vous de visite (membre)",
            description = "Créneau standard : rendez-vous créé au statut « demande », à confirmer par l'agent. "
                    + "Créneau déjà pris : 409. Créneau hors grille ou passé : 422.")
    public RendezVousResume reserver(@AuthenticationPrincipal Jwt jeton, @Valid @RequestBody RequeteRendezVous requete,
                                     HttpServletRequest http) {
        return service.reserver(identifiant(jeton), requete, adresseIp(http));
    }

    @PatchMapping("/rendez-vous/{id}/confirmer")
    @Operation(summary = "Confirmer une demande (agent du rendez-vous)", description = "Transition demande → confirme.")
    public RendezVousResume confirmer(@AuthenticationPrincipal Jwt jeton, @PathVariable Integer id, HttpServletRequest http) {
        return service.confirmer(id, identifiant(jeton), adresseIp(http));
    }

    @PatchMapping("/rendez-vous/{id}/annuler")
    @Operation(summary = "Annuler un rendez-vous (son membre ou son agent)",
            description = "Transition demande/confirme → annule, possible uniquement avant la date de la visite.")
    public RendezVousResume annuler(@AuthenticationPrincipal Jwt jeton, @PathVariable Integer id, HttpServletRequest http) {
        return service.annuler(id, identifiant(jeton), adresseIp(http));
    }

    @PatchMapping("/rendez-vous/{id}/honorer")
    @Operation(summary = "Signaler qu'une visite a eu lieu (agent du rendez-vous)",
            description = "Transition confirme → honore, possible uniquement après la date de la visite.")
    public RendezVousResume honorer(@AuthenticationPrincipal Jwt jeton, @PathVariable Integer id, HttpServletRequest http) {
        return service.honorer(id, identifiant(jeton), adresseIp(http));
    }

    private static Integer identifiant(Jwt jeton) {
        return Integer.valueOf(jeton.getSubject());
    }
}
