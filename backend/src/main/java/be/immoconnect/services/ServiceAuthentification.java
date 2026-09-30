package be.immoconnect.services;

import be.immoconnect.dto.ReponseJeton;
import be.immoconnect.dto.RequeteCode;
import be.immoconnect.dto.RequeteConnexion;
import be.immoconnect.dto.RequeteInscription;
import be.immoconnect.dto.RequeteReinitialisation;
import be.immoconnect.dto.UtilisateurResume;
import be.immoconnect.entities.Jeton;
import be.immoconnect.entities.Langue;
import be.immoconnect.entities.Membre;
import be.immoconnect.entities.TypeJeton;
import be.immoconnect.entities.Utilisateur;
import be.immoconnect.exceptions.CompteNonActiveException;
import be.immoconnect.exceptions.RessourceIntrouvableException;
import be.immoconnect.notification.EvenementCompte;
import be.immoconnect.repositories.LangueRepository;
import be.immoconnect.repositories.UtilisateurRepository;
import be.immoconnect.security.GardeConnexion;
import be.immoconnect.security.ProprietesSecurite;
import be.immoconnect.security.ServiceJeton;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cas d'utilisation S'inscrire (V7), Se connecter (M9), Récupérer son mot de passe (M10) et gestion
 * de la session (livrable 16, §2).
 * <p>
 * Une session tient en deux jetons : un jeton d'accès JWT de courte durée, gardé en mémoire par le
 * navigateur, et un jeton de rafraîchissement à usage unique, placé dans un cookie que les scripts ne
 * peuvent pas lire. Le second renouvelle le premier ; il est remplacé à chaque usage.
 */
@Service
public class ServiceAuthentification {

    /** Résultat d'une connexion : une session ouverte, ou un défi à relever d'abord. */
    public sealed interface Resultat permits Session, Defi {
    }

    /** @param rafraichissement valeur du jeton de rafraîchissement, à placer dans le cookie de session */
    public record Session(ReponseJeton reponse, String rafraichissement) implements Resultat {
    }

    public record Defi(String valeur, long expireDans) implements Resultat {
    }

    /** Résultat d'une inscription : une session, ou l'attente de la confirmation de l'adresse. */
    public record Inscription(ReponseJeton reponse, Session session) {
    }

    private final UtilisateurRepository utilisateurs;
    private final LangueRepository langues;
    private final PasswordEncoder encodeur;
    private final AuthenticationManager gestionnaire;
    private final ServiceJeton jetonsAcces;
    private final ServiceJetonsUniques jetons;
    private final GardeConnexion garde;
    private final ProprietesSecurite proprietes;
    private final ApplicationEventPublisher evenements;
    private final ServiceAudit audit;
    private final Clock horloge;

    public ServiceAuthentification(UtilisateurRepository utilisateurs, LangueRepository langues, PasswordEncoder encodeur,
                                   AuthenticationManager gestionnaire, ServiceJeton jetonsAcces, ServiceJetonsUniques jetons,
                                   GardeConnexion garde, ProprietesSecurite proprietes, ApplicationEventPublisher evenements,
                                   ServiceAudit audit, Clock horloge) {
        this.utilisateurs = utilisateurs;
        this.langues = langues;
        this.encodeur = encodeur;
        this.gestionnaire = gestionnaire;
        this.jetonsAcces = jetonsAcces;
        this.jetons = jetons;
        this.garde = garde;
        this.proprietes = proprietes;
        this.evenements = evenements;
        this.audit = audit;
        this.horloge = horloge;
    }

    /**
     * Crée un compte membre (rôle minimal par défaut — livrable 16 §1). Si l'activation par e-mail
     * est en vigueur, le compte attend la confirmation de son adresse ; sinon il est connecté.
     */
    @Transactional
    public Inscription inscrire(RequeteInscription requete, String ip) {
        String email = requete.email().trim().toLowerCase(Locale.ROOT);
        if (utilisateurs.existsByEmailIgnoreCase(email)) {
            throw new EmailDejaUtiliseException();
        }
        String codeLangue = (requete.langue() == null || requete.langue().isBlank()) ? "fr" : requete.langue();
        Langue langue = langues.findByCode(codeLangue).orElseThrow(() -> new RessourceIntrouvableException("Langue", codeLangue));
        String telephone = (requete.telephone() == null || requete.telephone().isBlank()) ? null : requete.telephone().trim();

        Membre membre = new Membre(requete.nom().trim(), requete.prenom().trim(), email,
                encodeur.encode(requete.motDePasse()), langue, telephone);
        membre.setCguAccepteesLe(maintenant());
        membre.setConsentementCommunications(Boolean.TRUE.equals(requete.consentementCommunications()));
        membre.setEmailVerifie(!proprietes.activationParCourriel());
        utilisateurs.save(membre);
        audit.enregistrer(membre, "inscription", "utilisateur#" + membre.getId(), ip);

        if (proprietes.activationParCourriel()) {
            envoyerActivation(membre, ip);
            return new Inscription(ReponseJeton.enAttenteActivation(UtilisateurResume.depuis(membre)), null);
        }
        Session session = ouvrirSession(membre, ip);
        return new Inscription(session.reponse(), session);
    }

    /**
     * Vérifie les identifiants. Un échec lève une exception d'authentification (401, message générique)
     * et compte pour le verrouillage ; la transaction n'est pas annulée pour autant, sinon le compteur
     * d'échecs ne serait jamais enregistré.
     */
    @Transactional(noRollbackFor = {AuthenticationException.class, CompteNonActiveException.class})
    public Resultat connecter(RequeteConnexion requete, String ip) {
        String email = requete.email().trim();
        garde.admettre(ip);
        Utilisateur compte = utilisateurs.findByEmailIgnoreCase(email).orElse(null);
        garde.admettre(email, compte);
        try {
            gestionnaire.authenticate(new UsernamePasswordAuthenticationToken(email, requete.motDePasse()));
        } catch (BadCredentialsException e) {
            garde.noterEchec(email, compte);
            if (compte != null) {
                audit.enregistrer(compte, "echec_connexion", "utilisateur#" + compte.getId(), ip);
            }
            throw e;
        }
        Utilisateur utilisateur = compte != null ? compte : utilisateurs.findByEmailIgnoreCase(email).orElseThrow();
        garde.noterSucces(utilisateur);
        if (!utilisateur.isEmailVerifie() && proprietes.activationParCourriel()) {
            throw new CompteNonActiveException();
        }
        if (proprietes.doubleFacteur() && utilisateur.exigeDoubleFacteur()) {
            ServiceJetonsUniques.Defi defi = jetons.emettreDefi(utilisateur, ip);
            evenements.publishEvent(new EvenementCompte(EvenementCompte.Type.code_connexion, utilisateur.getId(), defi.code()));
            audit.enregistrer(utilisateur, "defi_double_facteur", "utilisateur#" + utilisateur.getId(), ip);
            return new Defi(defi.valeur(), ServiceJetonsUniques.DUREE_DEFI.toSeconds());
        }
        audit.enregistrer(utilisateur, "connexion", "utilisateur#" + utilisateur.getId(), ip);
        return ouvrirSession(utilisateur, ip);
    }

    /** Second facteur : le bon code ouvre la session. */
    @Transactional
    public Session validerCode(RequeteCode requete, String ip) {
        garde.admettre(ip);
        Jeton defi = jetons.verifierCode(requete.defi(), requete.code());
        Utilisateur utilisateur = defi.getUtilisateur();
        audit.enregistrer(utilisateur, "connexion", "utilisateur#" + utilisateur.getId(), ip);
        return ouvrirSession(utilisateur, ip);
    }

    /** Renouvelle le jeton d'accès à partir du cookie de session, qui est lui-même remplacé. */
    @Transactional
    public Session rafraichir(String cookie, String ip) {
        Jeton jeton = jetons.consommer(cookie, TypeJeton.rafraichissement);
        Utilisateur utilisateur = jeton.getUtilisateur();
        if (!utilisateur.isActif()) {
            throw new BadCredentialsException("Compte désactivé");
        }
        return ouvrirSession(utilisateur, ip);
    }

    /** Ferme la session du cookie présenté ; un cookie inconnu ou déjà fermé est ignoré. */
    @Transactional
    public void deconnecter(String cookie, String ip) {
        if (cookie == null || cookie.isBlank()) {
            return;
        }
        try {
            Jeton jeton = jetons.consommer(cookie, TypeJeton.rafraichissement);
            audit.enregistrer(jeton.getUtilisateur(), "deconnexion", "utilisateur#" + jeton.getUtilisateur().getId(), ip);
        } catch (RuntimeException e) {
            // Déjà fermée, expirée ou inconnue : se déconnecter n'échoue jamais
        }
    }

    /**
     * Mot de passe oublié (M10) : la réponse est la même que l'adresse soit connue ou non, pour ne pas
     * révéler la base des comptes. Le lien, valable trente minutes, part par e-mail.
     */
    @Transactional
    public void demanderReinitialisation(String email, String ip) {
        garde.admettre(ip);
        utilisateurs.findByEmailIgnoreCase(email.trim()).filter(Utilisateur::isActif).ifPresent(utilisateur -> {
            String jeton = jetons.emettre(utilisateur, TypeJeton.reinitialisation, ServiceJetonsUniques.DUREE_REINITIALISATION, ip);
            evenements.publishEvent(new EvenementCompte(EvenementCompte.Type.reinitialisation, utilisateur.getId(), jeton));
            audit.enregistrer(utilisateur, "demande_reinitialisation", "utilisateur#" + utilisateur.getId(), ip);
        });
    }

    /** Nouveau mot de passe : le jeton est consommé, toutes les sessions sont fermées, l'utilisateur est prévenu. */
    @Transactional
    public void reinitialiser(RequeteReinitialisation requete, String ip) {
        Jeton jeton = jetons.consommer(requete.jeton(), TypeJeton.reinitialisation);
        Utilisateur utilisateur = jeton.getUtilisateur();
        utilisateur.setMotDePasse(encodeur.encode(requete.nouveauMotDePasse()));
        garde.noterSucces(utilisateur);
        jetons.fermerTous(utilisateur.getId(), TypeJeton.rafraichissement);
        audit.enregistrer(utilisateur, "reinitialisation_mot_de_passe", "utilisateur#" + utilisateur.getId(), ip);
        evenements.publishEvent(new EvenementCompte(EvenementCompte.Type.mot_de_passe_modifie, utilisateur.getId(), null));
    }

    /** Le lien d'activation confirme l'adresse et ouvre la première session. */
    @Transactional
    public Session activer(String valeur, String ip) {
        Jeton jeton = jetons.consommer(valeur, TypeJeton.activation);
        Utilisateur utilisateur = jeton.getUtilisateur();
        utilisateur.setEmailVerifie(true);
        audit.enregistrer(utilisateur, "activation", "utilisateur#" + utilisateur.getId(), ip);
        return ouvrirSession(utilisateur, ip);
    }

    /** Renvoie le lien d'activation ; réponse identique que l'adresse soit connue, activée ou non. */
    @Transactional
    public void renvoyerActivation(String email, String ip) {
        garde.admettre(ip);
        utilisateurs.findByEmailIgnoreCase(email.trim())
                .filter(utilisateur -> !utilisateur.isEmailVerifie())
                .ifPresent(utilisateur -> envoyerActivation(utilisateur, ip));
    }

    @Transactional(readOnly = true)
    public UtilisateurResume profil(Integer id) {
        return utilisateurs.findById(id).map(UtilisateurResume::depuis)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur", id));
    }

    private void envoyerActivation(Utilisateur utilisateur, String ip) {
        String jeton = jetons.emettre(utilisateur, TypeJeton.activation, ServiceJetonsUniques.DUREE_ACTIVATION, ip);
        evenements.publishEvent(new EvenementCompte(EvenementCompte.Type.activation, utilisateur.getId(), jeton));
    }

    private Session ouvrirSession(Utilisateur utilisateur, String ip) {
        ServiceJeton.Jeton acces = jetonsAcces.generer(utilisateur);
        String rafraichissement = jetons.emettre(utilisateur, TypeJeton.rafraichissement, proprietes.dureeSession(), ip);
        return new Session(ReponseJeton.bearer(acces.valeur(), acces.expireDans(), UtilisateurResume.depuis(utilisateur)), rafraichissement);
    }

    private LocalDateTime maintenant() {
        return LocalDateTime.now(horloge);
    }

    /** Conflit : l'adresse e-mail est déjà associée à un compte (409). */
    public static class EmailDejaUtiliseException extends IllegalStateException {
        public EmailDejaUtiliseException() {
            super("Un compte existe déjà pour cette adresse e-mail");
        }
    }
}
