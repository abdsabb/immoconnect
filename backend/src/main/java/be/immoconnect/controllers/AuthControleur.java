package be.immoconnect.controllers;

import static be.immoconnect.controllers.RequeteHttp.adresseIp;

import be.immoconnect.dto.ReponseDefi;
import be.immoconnect.dto.ReponseJeton;
import be.immoconnect.dto.RequeteAdresseCourriel;
import be.immoconnect.dto.RequeteChangementMotDePasse;
import be.immoconnect.dto.RequeteCode;
import be.immoconnect.dto.RequeteConnexion;
import be.immoconnect.dto.RequeteInscription;
import be.immoconnect.dto.RequeteJeton;
import be.immoconnect.dto.RequeteModificationProfil;
import be.immoconnect.dto.RequeteReinitialisation;
import be.immoconnect.dto.UtilisateurResume;
import be.immoconnect.security.ProprietesSecurite;
import be.immoconnect.services.ServiceAuthentification;
import be.immoconnect.services.ServiceAuthentification.Defi;
import be.immoconnect.services.ServiceAuthentification.Session;
import be.immoconnect.services.ServiceProfil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentification (livrable 15, §6.1 ; livrable 16, §2) : inscription, connexion et second facteur,
 * session, mot de passe oublié, activation, profil.
 * <p>
 * Le cookie de session porte le jeton de rafraîchissement : HttpOnly (illisible par un script),
 * Secure (HTTPS seulement), SameSite=Strict (jamais envoyé depuis un autre site) et limité au chemin
 * /api/v1/auth. Le jeton d'accès, lui, voyage dans le corps de la réponse.
 */
@RestController
@RequestMapping(AuthControleur.CHEMIN)
@Tag(name = "Authentification", description = "Inscription, connexion (jeton JWT et second facteur), session et identité")
public class AuthControleur {

    static final String CHEMIN = "/api/v1/auth";
    static final String COOKIE = "immoconnect_session";

    private final ServiceAuthentification service;
    private final ServiceProfil profil;
    private final ProprietesSecurite proprietes;

    public AuthControleur(ServiceAuthentification service, ServiceProfil profil, ProprietesSecurite proprietes) {
        this.service = service;
        this.profil = profil;
        this.proprietes = proprietes;
    }

    @PostMapping("/register")
    @Operation(summary = "Créer un compte membre",
            description = "Le compte est créé avec le rôle membre. Selon la configuration, il est connecté immédiatement "
                    + "ou attend la confirmation de son adresse e-mail (activationRequise).")
    public ResponseEntity<ReponseJeton> inscrire(@Valid @RequestBody RequeteInscription requete, HttpServletRequest http) {
        ServiceAuthentification.Inscription inscription = service.inscrire(requete, adresseIp(http));
        ResponseEntity.BodyBuilder reponse = ResponseEntity.status(HttpStatus.CREATED);
        if (inscription.session() != null) {
            reponse.header(HttpHeaders.SET_COOKIE, cookie(inscription.session().rafraichissement(), proprietes.dureeSession()).toString());
        }
        return reponse.body(inscription.reponse());
    }

    @PostMapping("/login")
    @Operation(summary = "Se connecter",
            description = "200 : jeton Bearer de courte durée et cookie de session. 202 : un code a été envoyé par e-mail, "
                    + "à présenter à /auth/login/code avec le défi renvoyé. 429 : trop de tentatives, voir Retry-After.")
    public ResponseEntity<?> connecter(@Valid @RequestBody RequeteConnexion requete, HttpServletRequest http) {
        return switch (service.connecter(requete, adresseIp(http))) {
            case Session session -> session(session);
            case Defi defi -> ResponseEntity.accepted().body(ReponseDefi.de(defi.valeur(), defi.expireDans()));
        };
    }

    @PostMapping("/login/code")
    @Operation(summary = "Second facteur", description = "Le défi de /auth/login et le code reçu par e-mail ouvrent la session.")
    public ResponseEntity<ReponseJeton> validerCode(@Valid @RequestBody RequeteCode requete, HttpServletRequest http) {
        return session(service.validerCode(requete, adresseIp(http)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renouveler le jeton d'accès",
            description = "À partir du cookie de session, qui est remplacé. Sans cookie valable : 401.")
    public ResponseEntity<ReponseJeton> rafraichir(@CookieValue(name = COOKIE, required = false) String cookie, HttpServletRequest http) {
        return session(service.rafraichir(cookie, adresseIp(http)));
    }

    @PostMapping("/logout")
    @Operation(summary = "Se déconnecter", description = "Ferme la session du cookie et l'efface.")
    public ResponseEntity<Void> deconnecter(@CookieValue(name = COOKIE, required = false) String cookie, HttpServletRequest http) {
        service.deconnecter(cookie, adresseIp(http));
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString()).build();
    }

    @PostMapping("/mot-de-passe-oublie")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Mot de passe oublié",
            description = "Envoie un lien de réinitialisation si l'adresse correspond à un compte. La réponse est la même dans tous les cas.")
    public void motDePasseOublie(@Valid @RequestBody RequeteAdresseCourriel requete, HttpServletRequest http) {
        service.demanderReinitialisation(requete.email(), adresseIp(http));
    }

    @PostMapping("/reinitialisation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Choisir un nouveau mot de passe", description = "Avec le jeton reçu par e-mail ; toutes les sessions sont fermées.")
    public void reinitialiser(@Valid @RequestBody RequeteReinitialisation requete, HttpServletRequest http) {
        service.reinitialiser(requete, adresseIp(http));
    }

    @PostMapping("/activation")
    @Operation(summary = "Activer un compte", description = "Confirme l'adresse e-mail avec le jeton reçu, et ouvre la session.")
    public ResponseEntity<ReponseJeton> activer(@Valid @RequestBody RequeteJeton requete, HttpServletRequest http) {
        return session(service.activer(requete.jeton(), adresseIp(http)));
    }

    @PostMapping("/activation/renvoi")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Renvoyer le lien d'activation", description = "La réponse est la même que l'adresse soit connue ou non.")
    public void renvoyerActivation(@Valid @RequestBody RequeteAdresseCourriel requete, HttpServletRequest http) {
        service.renvoyerActivation(requete.email(), adresseIp(http));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "jwt")
    @Operation(summary = "Profil de l'utilisateur connecté")
    public UtilisateurResume moi(@AuthenticationPrincipal Jwt jeton) {
        return service.profil(Integer.valueOf(jeton.getSubject()));
    }

    @PatchMapping("/me")
    @SecurityRequirement(name = "jwt")
    @Operation(summary = "Modifier son profil", description = "Nom, prénom, téléphone, langue, double facteur et consentement (cas M6).")
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
    @SecurityRequirement(name = "jwt")
    @Operation(summary = "Se désinscrire (droit à l'oubli)",
            description = "Soft delete (règle RA11) : favoris supprimés, messages vidés, identité anonymisée ; "
                    + "rendez-vous, paiements et journal d'audit conservés sans identité.")
    public ResponseEntity<Void> desinscrire(@AuthenticationPrincipal Jwt jeton, HttpServletRequest http) {
        profil.desinscrire(Integer.valueOf(jeton.getSubject()), adresseIp(http));
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString()).build();
    }

    private ResponseEntity<ReponseJeton> session(Session session) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie(session.rafraichissement(), proprietes.dureeSession()).toString())
                .body(session.reponse());
    }

    private ResponseCookie cookie(String valeur, Duration duree) {
        return ResponseCookie.from(COOKIE, valeur)
                .httpOnly(true)
                .secure(proprietes.cookieSecurise())
                .sameSite("Strict")
                .path(CHEMIN)
                .maxAge(duree)
                .build();
    }
}
