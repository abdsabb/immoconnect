package be.immoconnect.security;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adaptateur du service Have I Been Pwned, qui recense les mots de passe des fuites publiques.
 * <p>
 * Le mot de passe ne quitte jamais le serveur : seuls les cinq premiers caractères de son empreinte
 * SHA-1 sont envoyés (k-anonymat), le service renvoie toutes les empreintes qui commencent ainsi et la
 * comparaison se fait ici. L'adresse appelée est fixe : de ce que saisit l'utilisateur, seuls ces cinq
 * caractères hexadécimaux entrent dans l'URL.
 * <p>
 * Si le service ne répond pas, l'inscription n'est pas bloquée : la liste embarquée a déjà été consultée.
 */
public class MotsDePasseFuites implements MotsDePasseCompromis {

    private static final Logger journal = LoggerFactory.getLogger(MotsDePasseFuites.class);
    private static final String SERVICE = "https://api.pwnedpasswords.com/range/";
    private static final Duration DELAI = Duration.ofSeconds(2);

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(DELAI).build();
    private final MotsDePasseCompromis listeEmbarquee;

    public MotsDePasseFuites(MotsDePasseCompromis listeEmbarquee) {
        this.listeEmbarquee = listeEmbarquee;
    }

    @Override
    public boolean contient(String motDePasse) {
        if (listeEmbarquee.contient(motDePasse)) {
            return true;
        }
        String empreinte = sha1(motDePasse);
        String prefixe = empreinte.substring(0, 5);
        String suffixe = empreinte.substring(5);
        try {
            HttpRequest requete = HttpRequest.newBuilder(URI.create(SERVICE + prefixe))
                    .timeout(DELAI).header("Add-Padding", "true").GET().build();
            HttpResponse<String> reponse = client.send(requete, HttpResponse.BodyHandlers.ofString());
            if (reponse.statusCode() != 200) {
                journal.warn("Mots de passe compromis : le service a répondu {}", reponse.statusCode());
                return false;
            }
            // Chaque ligne : « SUFFIXE:NOMBRE » ; un nombre nul est du remplissage
            return reponse.body().lines().map(ligne -> ligne.split(":"))
                    .anyMatch(l -> l.length == 2 && l[0].equalsIgnoreCase(suffixe) && !"0".equals(l[1].strip()));
        } catch (IOException e) {
            journal.warn("Mots de passe compromis : service injoignable ({})", e.getMessage());
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static String sha1(String valeur) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(valeur.getBytes(StandardCharsets.UTF_8)))
                    .toUpperCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 indisponible", e);
        }
    }
}
