package be.immoconnect.securite;

import be.immoconnect.entite.Utilisateur;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Génère le jeton d'accès d'un utilisateur : sujet = identifiant, claims rôle et e-mail, expiration courte. */
@Service
public class ServiceJeton {

    private final JwtEncoder encodeur;
    private final ProprietesJwt proprietes;

    public ServiceJeton(JwtEncoder encodeur, ProprietesJwt proprietes) {
        this.encodeur = encodeur;
        this.proprietes = proprietes;
    }

    public record Jeton(String valeur, long expireDans) {
    }

    public Jeton generer(Utilisateur utilisateur) {
        Instant maintenant = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("immoconnect")
                .issuedAt(maintenant)
                .expiresAt(maintenant.plusSeconds(proprietes.dureeSecondes()))
                .subject(String.valueOf(utilisateur.getId()))
                .claim(ConfigurationJwt.CLAIM_ROLE, utilisateur.getRole())
                .claim("email", utilisateur.getEmail())
                .build();
        JwsHeader entete = JwsHeader.with(MacAlgorithm.HS256).build();
        String valeur = encodeur.encode(JwtEncoderParameters.from(entete, claims)).getTokenValue();
        return new Jeton(valeur, proprietes.dureeSecondes());
    }
}
