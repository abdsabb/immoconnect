package be.immoconnect.controllers;

import static be.immoconnect.controllers.RequeteHttp.adresseIp;

import be.immoconnect.dto.RequeteSignalement;
import be.immoconnect.dto.SignalementResume;
import be.immoconnect.security.ConfigurationJwt;
import be.immoconnect.services.ServiceSignalements;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Signalement d'un contenu par un utilisateur connecté (chapitre 11, règlement sur les services numériques). */
@RestController
@RequestMapping("/api/v1/signalements")
@Tag(name = "Signalements", description = "Signaler un message, une annonce ou un article")
@SecurityRequirement(name = "jwt")
public class SignalementControleur {

    private final ServiceSignalements service;

    public SignalementControleur(ServiceSignalements service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Signaler un contenu",
            description = "Un message reçu, une annonce ou un article. Un seul signalement ouvert par contenu et par personne ; l'administrateur tranche et motive sa décision.")
    public SignalementResume signaler(@AuthenticationPrincipal Jwt jeton, @Valid @RequestBody RequeteSignalement requete,
                                      HttpServletRequest http) {
        return service.signaler(Integer.valueOf(jeton.getSubject()), jeton.getClaimAsString(ConfigurationJwt.CLAIM_ROLE),
                requete, adresseIp(http));
    }
}
