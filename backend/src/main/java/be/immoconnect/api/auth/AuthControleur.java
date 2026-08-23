package be.immoconnect.api.auth;

import be.immoconnect.service.ServiceAuthentification;
import be.immoconnect.service.ServiceProfil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Authentification (livrable 15, §6.1) : /auth/register, /auth/login, /auth/me. */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentification", description = "Inscription, connexion (jeton JWT) et identité de l'utilisateur connecté")
public class AuthControleur {

    private final ServiceAuthentification service;
    private final ServiceProfil profil;

    public AuthControleur(ServiceAuthentification service, ServiceProfil profil) {
        this.service = service;
        this.profil = profil;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer un compte membre", description = "Le compte est créé avec le rôle membre et connecté immédiatement.")
    public ReponseJeton inscrire(@Valid @RequestBody RequeteInscription requete, HttpServletRequest http) {
        return service.inscrire(requete, adresseIp(http));
    }

    @PostMapping("/login")
    @Operation(summary = "Obtenir un jeton JWT", description = "Jeton Bearer valable 1 h, à transmettre dans l'en-tête Authorization.")
    public ReponseJeton connecter(@Valid @RequestBody RequeteConnexion requete, HttpServletRequest http) {
        return service.connecter(requete, adresseIp(http));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "jwt")
    @Operation(summary = "Profil de l'utilisateur connecté")
    public UtilisateurResume moi(@AuthenticationPrincipal Jwt jeton) {
        return service.profil(Integer.valueOf(jeton.getSubject()));
    }

    @PatchMapping("/me")
    @SecurityRequirement(name = "jwt")
    @Operation(summary = "Modifier son profil", description = "Nom, prénom, téléphone et langue préférée (cas M6).")
    public UtilisateurResume modifierProfil(@AuthenticationPrincipal Jwt jeton, @Valid @RequestBody RequeteModificationProfil requete,
                                            HttpServletRequest http) {
        return profil.modifier(Integer.valueOf(jeton.getSubject()), requete, adresseIp(http));
    }

    @PutMapping("/me/mot-de-passe")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "jwt")
    @Operation(summary = "Changer son mot de passe", description = "L'ancien mot de passe est exigé.")
    public void changerMotDePasse(@AuthenticationPrincipal Jwt jeton, @Valid @RequestBody RequeteChangementMotDePasse requete,
                                  HttpServletRequest http) {
        profil.changerMotDePasse(Integer.valueOf(jeton.getSubject()), requete, adresseIp(http));
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "jwt")
    @Operation(summary = "Se désinscrire (droit à l'oubli)",
            description = "Soft delete (règle RA11) : favoris supprimés, messages vidés, identité anonymisée ; "
                    + "rendez-vous, paiements et journal d'audit conservés sans identité.")
    public void desinscrire(@AuthenticationPrincipal Jwt jeton, HttpServletRequest http) {
        profil.desinscrire(Integer.valueOf(jeton.getSubject()), adresseIp(http));
    }

    /** Adresse IP réelle, en tenant compte du proxy Nginx (X-Forwarded-For) — pour le journal d'audit. */
    private static String adresseIp(HttpServletRequest http) {
        String transmise = http.getHeader("X-Forwarded-For");
        return transmise != null && !transmise.isBlank() ? transmise.split(",")[0].trim() : http.getRemoteAddr();
    }
}
