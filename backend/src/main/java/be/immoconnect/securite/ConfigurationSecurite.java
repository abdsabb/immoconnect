package be.immoconnect.securite;

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
    SecurityFilterChain chaineDeFiltres(HttpSecurity http, JwtAuthenticationConverter convertisseur) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Documentation de l'API et supervision
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // Niveau d'accès « public » (livrable 15, §4) : consultation et authentification
                .requestMatchers(HttpMethod.GET, "/api/v1/biens/**", "/api/v1/articles/**",
                        "/api/v1/traductions/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                // Tout le reste exige un jeton valide
                .anyRequest().authenticated())
            .oauth2ResourceServer(serveur -> serveur.jwt(jwt -> jwt.jwtAuthenticationConverter(convertisseur)));
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
