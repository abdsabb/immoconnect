package be.immoconnect.security;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;

/** Liste embarquée des mots de passe les plus répandus : aucun appel réseau, comparaison sans casse. */
public class ListeMotsDePasseCourants implements MotsDePasseCompromis {

    private final Set<String> courants;

    public ListeMotsDePasseCourants() {
        try (BufferedReader lecteur = new BufferedReader(new InputStreamReader(
                new ClassPathResource("securite/mots-de-passe-courants.txt").getInputStream(), StandardCharsets.UTF_8))) {
            this.courants = lecteur.lines().map(String::strip)
                    .filter(ligne -> !ligne.isEmpty() && !ligne.startsWith("#"))
                    .map(ligne -> ligne.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toUnmodifiableSet());
        } catch (IOException e) {
            throw new UncheckedIOException("Liste des mots de passe courants illisible", e);
        }
    }

    @Override
    public boolean contient(String motDePasse) {
        return courants.contains(motDePasse.toLowerCase(Locale.ROOT));
    }
}
