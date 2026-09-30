package be.immoconnect.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Réglages de la sécurité des comptes (immoconnect.securite.*).
 *
 * @param cookieSecurise          le cookie de session ne voyage qu'en HTTPS
 * @param dureeSession            durée de vie du jeton de rafraîchissement
 * @param activationParCourriel   un nouveau compte ne se connecte qu'après confirmation de son adresse
 * @param doubleFacteur           code par e-mail exigé des agents, des administrateurs et des membres qui l'ont choisi
 * @param echecsAvantVerrouillage nombre d'échecs de connexion tolérés avant le premier verrouillage
 * @param connexionsParMinute     tentatives de connexion acceptées par minute et par adresse IP
 * @param motsDePasseCompromis    « hibp » pour interroger Have I Been Pwned, « local » pour la seule liste embarquée
 */
@ConfigurationProperties(prefix = "immoconnect.securite")
public record ProprietesSecurite(boolean cookieSecurise, Duration dureeSession, boolean activationParCourriel,
                                 boolean doubleFacteur, int echecsAvantVerrouillage, int connexionsParMinute,
                                 String motsDePasseCompromis) {
}
