package be.immoconnect.securite;

import be.immoconnect.service.ServiceClesApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Configuration Spring Security de l'API (livrable 16, §1 à §3).
 * <ul>
 *   <li>API sans état : aucune session serveur, le JWT voyage dans l'en-tête Authorization ;</li>
 *   <li>CSRF désactivé : aucun cookie de session, la falsification de requête intersite est
 *       inopérante par conception ;</li>
 *   <li>sécurité par défaut : tout est fermé sauf les ressources publiques déclarées ici ;</li>
 *   <li>contrôle par rôle (RBAC) via les autorités ROLE_MEMBRE / ROLE_AGENT / ROLE_ADMIN portées par le jeton.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class ConfigurationSecurite {

    @Bean
    SecurityFilterChain chaineDeFiltres(HttpSecurity http, JwtAuthenticationConverter convertisseur, ServiceClesApi clesApi,
                                        LimiteDebit limiteDebit, ObjectMapper json) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Documentation de l'API et supervision
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // Rendez-vous : réserver est le fait d'un membre, confirmer et honorer celui d'un agent.
                // Déclaré avant la règle publique des biens, qui couvrirait sinon /biens/{id}/creneaux.
                .requestMatchers(HttpMethod.GET, "/api/v1/biens/*/creneaux").hasRole("MEMBRE")
                .requestMatchers(HttpMethod.POST, "/api/v1/rendez-vous").hasRole("MEMBRE")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/rendez-vous/*/confirmer", "/api/v1/rendez-vous/*/honorer").hasRole("AGENT")
                // Favoris : propres au membre connecté
                .requestMatchers("/api/v1/biens/*/favori", "/api/v1/membres/moi/**").hasRole("MEMBRE")
                // Paiement : le webhook est public mais authentifié par la signature de Stripe.
                .requestMatchers(HttpMethod.GET, "/api/v1/paiements/config").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/webhooks/stripe").permitAll()
                .requestMatchers("/api/v1/paiements/**").hasRole("MEMBRE")
                // Back-office de l'agent : ses annonces et leurs photos. Toute écriture sur un bien est le fait
                // d'un agent — les favoris, déclarés plus haut, sont déjà attribués au membre.
                .requestMatchers("/api/v1/agents/moi/**").hasRole("AGENT")
                .requestMatchers(HttpMethod.POST, "/api/v1/biens", "/api/v1/biens/**").hasRole("AGENT")
                .requestMatchers(HttpMethod.PUT, "/api/v1/biens/**").hasRole("AGENT")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/biens/**").hasRole("AGENT")
                // Back-office de l'administrateur
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                // API ouverte : pas de jeton d'utilisateur, l'accès est décidé par le filtre de clé API
                .requestMatchers(HttpMethod.GET, FiltreCleApi.CHEMIN + "**").permitAll()
                // Niveau d'accès « public » (livrable 15, §4) : consultation et authentification
                .requestMatchers(HttpMethod.GET, "/api/v1/biens/**", "/api/v1/articles/**",
                        "/api/v1/traductions/**", "/api/v1/categories", "/storage/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                // Tout le reste exige un jeton valide
                .anyRequest().authenticated())
            .oauth2ResourceServer(serveur -> serveur.jwt(jwt -> jwt.jwtAuthenticationConverter(convertisseur)))
            .addFilterBefore(new FiltreCleApi(clesApi, limiteDebit, json), AuthorizationFilter.class);
        return http.build();
    }

    /** Vérification e-mail / mot de passe à la connexion (BCrypt). */
    @Bean
    AuthenticationManager gestionnaireAuthentification(ServiceUtilisateurDetails details, PasswordEncoder encodeur) {
        DaoAuthenticationProvider fournisseur = new DaoAuthenticationProvider(details);
        fournisseur.setPasswordEncoder(encodeur);
        return new ProviderManager(fournisseur);
    }

    /** Hachage des mots de passe : BCrypt, facteur de coût 12 (livrable 16, §2.1). */
    @Bean
    PasswordEncoder encodeurMotDePasse() {
        return new BCryptPasswordEncoder(12);
    }
}
