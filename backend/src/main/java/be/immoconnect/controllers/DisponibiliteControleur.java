package be.immoconnect.controllers;

import static be.immoconnect.controllers.RequeteHttp.adresseIp;

import be.immoconnect.dto.DisponibiliteVue;
import be.immoconnect.dto.RequeteDisponibilite;
import be.immoconnect.dto.RetraitDisponibilites;
import be.immoconnect.services.ServiceDisponibilites;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Disponibilités de l'agent connecté (cas AG7). Le rôle est contrôlé par la configuration de sécurité ;
 * la propriété de chaque plage l'est par le service.
 */
@RestController
@RequestMapping("/api/v1/disponibilites")
@SecurityRequirement(name = "jwt")
@Tag(name = "Disponibilités", description = "Plages pendant lesquelles un agent reçoit en visite")
public class DisponibiliteControleur {

    private final ServiceDisponibilites service;

    public DisponibiliteControleur(ServiceDisponibilites service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Mes disponibilités (agent)",
            description = "Plages ouvertes par l'agent connecté. Période par défaut : de la semaine passée aux 60 prochains jours.")
    public List<DisponibiliteVue> lister(
            @AuthenticationPrincipal Jwt jeton,
            @Parameter(description = "Premier jour (AAAA-MM-JJ)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @Parameter(description = "Dernier jour inclus (AAAA-MM-JJ)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au) {
        return service.lister(identifiant(jeton), du, au);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Ouvrir une plage de disponibilité (agent)",
            description = "La plage tient dans une journée, entre 8 h et 21 h, par demi-heures, et dure une heure au moins. "
                    + "« semaines » la répète chaque semaine (8 au plus). Chevauchement ou plage invalide : 422.")
    public List<DisponibiliteVue> ouvrir(@AuthenticationPrincipal Jwt jeton, @Valid @RequestBody RequeteDisponibilite requete,
                                         HttpServletRequest http) {
        return service.ouvrir(identifiant(jeton), requete, adresseIp(http));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Retirer une plage de disponibilité (son agent)",
            description = "Si des visites y sont prévues : 409 avec leur nombre. Avec annulerVisites=true, elles sont annulées, "
                    + "les membres prévenus par e-mail et les créneaux payés remboursés.")
    public RetraitDisponibilites retirer(@AuthenticationPrincipal Jwt jeton, @PathVariable Integer id,
                                         @RequestParam(defaultValue = "false") boolean annulerVisites, HttpServletRequest http) {
        return service.retirer(identifiant(jeton), id, annulerVisites, adresseIp(http));
    }

    @DeleteMapping
    @Operation(summary = "Se déclarer indisponible sur une période (agent)",
            description = "Retire toutes les plages à venir de l'agent entre deux jours, bornes incluses. Mêmes règles que le "
                    + "retrait d'une plage : 409 si des visites sont prévues, sauf avec annulerVisites=true.")
    public RetraitDisponibilites retirerPeriode(
            @AuthenticationPrincipal Jwt jeton,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
            @RequestParam(defaultValue = "false") boolean annulerVisites, HttpServletRequest http) {
        return service.retirerPeriode(identifiant(jeton), du, au, annulerVisites, adresseIp(http));
    }

    private static Integer identifiant(Jwt jeton) {
        return Integer.valueOf(jeton.getSubject());
    }
}
