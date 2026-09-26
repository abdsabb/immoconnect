package be.immoconnect.api.erreur;

import be.immoconnect.entite.RendezVous.TransitionInterditeException;
import be.immoconnect.service.BienIndisponibleException;
import be.immoconnect.service.CreneauIndisponibleException;
import be.immoconnect.service.DonneeInvalideException;
import be.immoconnect.service.OperationInterditeException;
import be.immoconnect.service.RessourceIntrouvableException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    /** Paramètre d'URL du mauvais type (identifiant non numérique, date mal formée) : 400, sans détail technique. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail parametreInvalide(MethodArgumentTypeMismatchException e) {
        return probleme(HttpStatus.BAD_REQUEST, "Requête invalide", "Le paramètre « " + e.getName() + " » est invalide", "requete-invalide");
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
