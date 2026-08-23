package be.immoconnect.securite;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuration Spring Security de l'API (livrable 16, §1 et §2).
 * <ul>
 *   <li>API sans état : aucune session serveur, le JWT arrivera en en-tête Authorization ;</li>
 *   <li>CSRF désactivé : l'API n'utilise pas de cookie de session, la falsification de requête
 *       intersite est inopérante par conception ;</li>
 *   <li>Sécurité par défaut : tout est fermé sauf les ressources publiques déclarées ici
 *       (consultation des biens, documentation Swagger, contrôle de santé).</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class ConfigurationSecurite {

    @Bean
    SecurityFilterChain chaineDeFiltres(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Documentation de l'API et supervision
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // Niveau d'accès « public » de l'API (livrable 15, §4) : consultation et authentification
                .requestMatchers(HttpMethod.GET, "/api/v1/biens/**", "/api/v1/articles/**",
                        "/api/v1/traductions/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                // Tout le reste exige une authentification (JWT — Sprint 1)
                .anyRequest().authenticated())
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    /** Hachage des mots de passe : BCrypt, facteur de coût 12 (livrable 16, §2.1). */
    @Bean
    PasswordEncoder encodeurMotDePasse() {
        return new BCryptPasswordEncoder(12);
    }
}
