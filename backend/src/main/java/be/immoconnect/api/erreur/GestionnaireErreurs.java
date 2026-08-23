package be.immoconnect.api.erreur;

import be.immoconnect.entite.RendezVous.TransitionInterditeException;
import be.immoconnect.service.RessourceIntrouvableException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    @ExceptionHandler({TransitionInterditeException.class, IllegalStateException.class})
    ProblemDetail conflit(RuntimeException e) {
        return probleme(HttpStatus.CONFLICT, "Conflit d'état", e.getMessage(), "conflit-etat");
    }

    /** Identifiants invalides : réponse 401 générique, sans révéler si l'adresse existe (livrable 16 §2.2). */
    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail nonAuthentifie(AuthenticationException e) {
        return probleme(HttpStatus.UNAUTHORIZED, "Non authentifié", "Identifiants invalides", "non-authentifie");
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
