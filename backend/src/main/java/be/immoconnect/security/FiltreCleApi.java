package be.immoconnect.security;

import be.immoconnect.services.ServiceClesApi;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Filtre Spring Security de l'API ouverte (/api/v1/open-data) : le consommateur externe s'identifie
 * par une clé dans l'en-tête X-API-Key, pas par un jeton d'utilisateur.
 * <ul>
 *   <li>clé absente, inconnue ou révoquée : 401, sans dire laquelle des trois (RA12) ;</li>
 *   <li>quota de la minute épuisé : 429, avec l'en-tête Retry-After.</li>
 * </ul>
 */
public class FiltreCleApi extends OncePerRequestFilter {

    public static final String EN_TETE = "X-API-Key";
    public static final String CHEMIN = "/api/v1/open-data/";

    private static final String BASE_TYPES = "https://www.immoconnect.be/erreurs/";

    private final ServiceClesApi cles;
    private final LimiteDebit limite;
    private final ObjectMapper json;

    public FiltreCleApi(ServiceClesApi cles, LimiteDebit limite, ObjectMapper json) {
        this.cles = cles;
        this.limite = limite;
        this.json = json;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest requete) {
        return !requete.getRequestURI().startsWith(CHEMIN);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requete, HttpServletResponse reponse, FilterChain chaine)
            throws ServletException, IOException {
        Optional<Integer> cle = cles.verifier(requete.getHeader(EN_TETE));
        if (cle.isEmpty()) {
            refuser(reponse, HttpStatus.UNAUTHORIZED, "Non authentifié", "Clé API manquante, invalide ou révoquée", "non-authentifie");
            return;
        }
        reponse.setHeader("X-RateLimit-Limit", String.valueOf(limite.appelsParMinute()));
        if (!limite.accepter(cle.get())) {
            reponse.setHeader("Retry-After", String.valueOf(limite.secondesAvantReprise()));
            refuser(reponse, HttpStatus.TOO_MANY_REQUESTS, "Quota dépassé",
                    "Limite de " + limite.appelsParMinute() + " appels par minute atteinte", "quota-depasse");
            return;
        }
        chaine.doFilter(requete, reponse);
    }

    /** Même format d'erreur que le reste de l'API : problem+json (RFC 7807). */
    private void refuser(HttpServletResponse reponse, HttpStatus statut, String titre, String detail, String type) throws IOException {
        ProblemDetail probleme = ProblemDetail.forStatusAndDetail(statut, detail);
        probleme.setTitle(titre);
        probleme.setType(URI.create(BASE_TYPES + type));
        reponse.setStatus(statut.value());
        reponse.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        reponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
        json.writeValue(reponse.getOutputStream(), probleme);
    }
}
