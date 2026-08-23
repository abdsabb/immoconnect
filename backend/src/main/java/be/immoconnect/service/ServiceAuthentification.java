package be.immoconnect.service;

import be.immoconnect.api.auth.ReponseJeton;
import be.immoconnect.api.auth.RequeteConnexion;
import be.immoconnect.api.auth.RequeteInscription;
import be.immoconnect.api.auth.UtilisateurResume;
import be.immoconnect.depot.LangueRepository;
import be.immoconnect.depot.UtilisateurRepository;
import be.immoconnect.entite.Langue;
import be.immoconnect.entite.Membre;
import be.immoconnect.entite.Utilisateur;
import be.immoconnect.securite.ServiceJeton;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cas d'utilisation S'inscrire (V7), Se connecter (M9) et consultation de son identité. */
@Service
public class ServiceAuthentification {

    private final UtilisateurRepository utilisateurs;
    private final LangueRepository langues;
    private final PasswordEncoder encodeur;
    private final AuthenticationManager gestionnaire;
    private final ServiceJeton jetons;
    private final ServiceAudit audit;

    public ServiceAuthentification(UtilisateurRepository utilisateurs, LangueRepository langues, PasswordEncoder encodeur,
                                   AuthenticationManager gestionnaire, ServiceJeton jetons, ServiceAudit audit) {
        this.utilisateurs = utilisateurs;
        this.langues = langues;
        this.encodeur = encodeur;
        this.gestionnaire = gestionnaire;
        this.jetons = jetons;
        this.audit = audit;
    }

    /** Crée un compte membre (rôle minimal par défaut — livrable 16 §1), puis le connecte. */
    @Transactional
    public ReponseJeton inscrire(RequeteInscription requete, String ip) {
        String email = requete.email().trim().toLowerCase();
        if (utilisateurs.existsByEmailIgnoreCase(email)) {
            throw new EmailDejaUtiliseException();
        }
        String codeLangue = (requete.langue() == null || requete.langue().isBlank()) ? "fr" : requete.langue();
        Langue langue = langues.findByCode(codeLangue).orElseThrow(() -> new RessourceIntrouvableException("Langue", codeLangue));
        String telephone = (requete.telephone() == null || requete.telephone().isBlank()) ? null : requete.telephone().trim();

        Membre membre = new Membre(requete.nom().trim(), requete.prenom().trim(), email,
                encodeur.encode(requete.motDePasse()), langue, telephone);
        utilisateurs.save(membre);
        audit.enregistrer(membre, "inscription", "utilisateur#" + membre.getId(), ip);
        return reponse(membre);
    }

    /** Vérifie les identifiants ; un échec lève BadCredentialsException (401, message générique). */
    @Transactional
    public ReponseJeton connecter(RequeteConnexion requete, String ip) {
        gestionnaire.authenticate(new UsernamePasswordAuthenticationToken(requete.email().trim(), requete.motDePasse()));
        Utilisateur utilisateur = utilisateurs.findByEmailIgnoreCase(requete.email().trim()).orElseThrow();
        audit.enregistrer(utilisateur, "connexion", "utilisateur#" + utilisateur.getId(), ip);
        return reponse(utilisateur);
    }

    @Transactional(readOnly = true)
    public UtilisateurResume profil(Integer id) {
        return utilisateurs.findById(id).map(UtilisateurResume::depuis)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur", id));
    }

    private ReponseJeton reponse(Utilisateur utilisateur) {
        ServiceJeton.Jeton jeton = jetons.generer(utilisateur);
        return ReponseJeton.bearer(jeton.valeur(), jeton.expireDans(), UtilisateurResume.depuis(utilisateur));
    }

    /** Conflit : l'adresse e-mail est déjà associée à un compte (409). */
    public static class EmailDejaUtiliseException extends IllegalStateException {
        public EmailDejaUtiliseException() {
            super("Un compte existe déjà pour cette adresse e-mail");
        }
    }
}
