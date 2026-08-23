package be.immoconnect.securite;

import be.immoconnect.depot.UtilisateurRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Charge un compte par e-mail pour la vérification du mot de passe à la connexion. */
@Service
public class ServiceUtilisateurDetails implements UserDetailsService {

    private final UtilisateurRepository utilisateurs;

    public ServiceUtilisateurDetails(UtilisateurRepository utilisateurs) {
        this.utilisateurs = utilisateurs;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        return utilisateurs.findByEmailIgnoreCase(email)
                .map(u -> User.withUsername(u.getEmail())
                        .password(u.getMotDePasse())
                        .roles(u.getRole().toUpperCase())
                        .build())
                // Message générique : ne jamais révéler si l'adresse existe (livrable 16, §2.2)
                .orElseThrow(() -> new UsernameNotFoundException("Identifiants invalides"));
    }
}
