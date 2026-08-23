package be.immoconnect.securite;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/**
 * Émission et vérification des jetons JWT signés HS256 (livrable 16, §2.1).
 * Le rôle porté par le jeton (membre, agent, admin) devient l'autorité Spring Security
 * ROLE_MEMBRE / ROLE_AGENT / ROLE_ADMIN utilisée par le contrôle d'accès.
 */
@Configuration
@EnableConfigurationProperties(ProprietesJwt.class)
public class ConfigurationJwt {

    public static final String CLAIM_ROLE = "role";

    @Bean
    SecretKey cleSecrete(ProprietesJwt proprietes) {
        if (proprietes.secret() == null || proprietes.secret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("immoconnect.jwt.secret doit faire au moins 32 octets (HS256)");
        }
        return new SecretKeySpec(proprietes.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtEncoder encodeurJwt(SecretKey cle) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(cle));
    }

    @Bean
    JwtDecoder decodeurJwt(SecretKey cle) {
        return NimbusJwtDecoder.withSecretKey(cle).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    JwtAuthenticationConverter convertisseurAuthentification() {
        JwtAuthenticationConverter convertisseur = new JwtAuthenticationConverter();
        convertisseur.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString(CLAIM_ROLE);
            return role == null ? List.<GrantedAuthority>of()
                    : List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
        });
        return convertisseur;
    }
}
