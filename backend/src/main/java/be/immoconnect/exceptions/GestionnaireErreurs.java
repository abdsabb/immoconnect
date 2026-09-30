package be.immoconnect.exceptions;

import be.immoconnect.entities.RendezVous.TransitionInterditeException;
import be.immoconnect.paiement.PasserellePaiement.PasserelleIndisponibleException;
import be.immoconnect.paiement.PasserellePaiement.SignatureInvalideException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * Conversion des exceptions en réponses « problem+json » (RFC 7807), conformément au livrable 15 :
 * 404 ressource introuvable, 409 conflit d'état (transition interdite), 422 validation échouée.
 * Les messages restent génériques : aucun détail technique ne fuit vers le client (livrable 16).
 */
@RestControllerAdvice
public class GestionnaireErreurs {

    private static final String BASE_TYPES = "https://www.immoconnect.be/erreurs/";

    @ExceptionHandler(RessourceIntrouvableException.class)
    ProblemDetail introuvable(RessourceIntrouvableException e) {
        return probleme(HttpStatus.NOT_FOUND, "Ressource introuvable", e.getMessage(), "introuvable");
    }

    @ExceptionHandler(OperationInterditeException.class)
    ProblemDetail interdit(OperationInterditeException e) {
        return probleme(HttpStatus.FORBIDDEN, "Opération interdite", e.getMessage(), "interdit");
    }

    @ExceptionHandler({TransitionInterditeException.class, IllegalStateException.class})
    ProblemDetail conflit(RuntimeException e) {
        return probleme(HttpStatus.CONFLICT, "Conflit d'état", e.getMessage(), "conflit-etat");
    }

    /** Scénario A2 : le créneau a été pris entre l'affichage de la grille et la réservation. */
    @ExceptionHandler(CreneauIndisponibleException.class)
    ProblemDetail creneauIndisponible(CreneauIndisponibleException e) {
        return probleme(HttpStatus.CONFLICT, "Créneau indisponible", e.getMessage(), "creneau-indisponible");
    }

    /** Cas d'erreur E3 : le bien a été retiré de la vente. */
    @ExceptionHandler(BienIndisponibleException.class)
    ProblemDetail bienIndisponible(BienIndisponibleException e) {
        return probleme(HttpStatus.CONFLICT, "Bien indisponible", e.getMessage(), "bien-indisponible");
    }

    /** Règle métier non respectée par une donnée : même forme qu'une validation de formulaire. */
    @ExceptionHandler(DonneeInvalideException.class)
    ProblemDetail donneeInvalide(DonneeInvalideException e) {
        ProblemDetail pd = probleme(HttpStatus.UNPROCESSABLE_CONTENT, "Validation échouée",
                "Certains champs sont invalides", "validation");
        pd.setProperty("champs", Map.of(e.getChamp(), e.getMessage()));
        return pd;
    }

    /** Le prestataire de paiement ne répond pas : l'opération est abandonnée, rien n'a été écrit. */
    @ExceptionHandler(PasserelleIndisponibleException.class)
    ProblemDetail paiementIndisponible(PasserelleIndisponibleException e) {
        return probleme(HttpStatus.BAD_GATEWAY, "Paiement indisponible", e.getMessage(), "paiement-indisponible");
    }

    /** Webhook sans signature valide : refusé, sans indiquer pourquoi (livrable 16). */
    @ExceptionHandler(SignatureInvalideException.class)
    ProblemDetail signatureInvalide(SignatureInvalideException e) {
        return probleme(HttpStatus.BAD_REQUEST, "Requête invalide", e.getMessage(), "requete-invalide");
    }

    /** Fichier téléversé au-delà de la limite (5 Mo par photo). */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail fichierTropVolumineux(MaxUploadSizeExceededException e) {
        return probleme(HttpStatus.CONTENT_TOO_LARGE, "Fichier trop volumineux", "Une photo pèse 5 Mo au plus", "fichier-trop-volumineux");
    }

    /** Requête multipart sans le fichier attendu, ou qui n'est pas multipart. */
    @ExceptionHandler({MissingServletRequestPartException.class, MissingServletRequestParameterException.class, MultipartException.class})
    ProblemDetail fichierManquant(Exception e) {
        return probleme(HttpStatus.BAD_REQUEST, "Requête invalide", "Un élément attendu manque à la requête", "requete-invalide");
    }

    /** Paramètre d'URL du mauvais type (identifiant non numérique, date mal formée) : 400, sans détail technique. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail parametreInvalide(MethodArgumentTypeMismatchException e) {
        return probleme(HttpStatus.BAD_REQUEST, "Requête invalide", "Le paramètre « " + e.getName() + " » est invalide", "requete-invalide");
    }

    /** Force brute : 429, avec le délai d'attente dans Retry-After. */
    @ExceptionHandler(TropDeTentativesException.class)
    ResponseEntity<ProblemDetail> tropDeTentatives(TropDeTentativesException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(e.getSecondesAvantReprise()))
                .body(probleme(HttpStatus.TOO_MANY_REQUESTS, "Trop de tentatives", e.getMessage(), "trop-de-tentatives"));
    }

    /** Lien ou code d'un e-mail inconnu, expiré ou déjà utilisé : 400, sans dire lequel. */
    @ExceptionHandler(JetonInvalideException.class)
    ProblemDetail jetonInvalide(JetonInvalideException e) {
        return probleme(HttpStatus.BAD_REQUEST, "Lien invalide", e.getMessage(), "lien-invalide");
    }

    /** Code du double facteur incorrect : 401, avec les essais restants. */
    @ExceptionHandler(CodeInvalideException.class)
    ProblemDetail codeInvalide(CodeInvalideException e) {
        ProblemDetail pd = probleme(HttpStatus.UNAUTHORIZED, "Code incorrect", e.getMessage(), "code-invalide");
        pd.setProperty("essaisRestants", e.getEssaisRestants());
        return pd;
    }

    /** Identifiants corrects mais adresse non confirmée : 403, le lien d'activation peut être renvoyé. */
    @ExceptionHandler(CompteNonActiveException.class)
    ProblemDetail compteNonActive(CompteNonActiveException e) {
        return probleme(HttpStatus.FORBIDDEN, "Compte non activé", e.getMessage(), "compte-non-active");
    }

    /** Identifiants invalides : réponse 401 générique, sans révéler si l'adresse existe (livrable 16 §2.2). */
    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail nonAuthentifie(AuthenticationException e) {
        return probleme(HttpStatus.UNAUTHORIZED, "Non authentifié", "Identifiants invalides", "non-authentifie");
    }

    /** Corps JSON absent, mal formé ou mal encodé : 400 sans aucun détail technique (livrable 16 §1). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail corpsIllisible(HttpMessageNotReadableException e) {
        return probleme(HttpStatus.BAD_REQUEST, "Requête invalide", "Le corps de la requête est illisible ou mal formé (JSON UTF-8 attendu)", "requete-invalide");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail requeteInvalide(IllegalArgumentException e) {
        return probleme(HttpStatus.BAD_REQUEST, "Requête invalide", e.getMessage(), "requete-invalide");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException e) {
        ProblemDetail pd = probleme(HttpStatus.UNPROCESSABLE_CONTENT, "Validation échouée",
                "Certains champs sont invalides", "validation");
        Map<String, String> champs = new LinkedHashMap<>();
        for (FieldError erreur : e.getBindingResult().getFieldErrors()) {
            champs.putIfAbsent(erreur.getField(), erreur.getDefaultMessage());
        }
        pd.setProperty("champs", champs);
        return pd;
    }

    private static ProblemDetail probleme(HttpStatus statut, String titre, String detail, String type) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(statut, detail);
        pd.setTitle(titre);
        pd.setType(URI.create(BASE_TYPES + type));
        return pd;
    }
}
