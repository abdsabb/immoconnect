package be.immoconnect.security;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Limitation de débit de l'API ouverte (livrable 15, §5) : un nombre d'appels par minute et par clé.
 * Compteur en mémoire à fenêtre fixe : suffisant pour un serveur unique, à remplacer par un compteur
 * partagé (Redis) si l'application tournait un jour sur plusieurs serveurs.
 */
@Component
public class LimiteDebit {

    private record Fenetre(long minute, int appels) {
    }

    private final ConcurrentHashMap<Integer, Fenetre> fenetres = new ConcurrentHashMap<>();
    private final int appelsParMinute;
    private final Clock horloge;

    public LimiteDebit(@Value("${immoconnect.api.appels-par-minute}") int appelsParMinute, Clock horloge) {
        this.appelsParMinute = appelsParMinute;
        this.horloge = horloge;
    }

    /** @return vrai si l'appel est accepté, faux si la clé a épuisé son quota de la minute en cours */
    public boolean accepter(Integer cleId) {
        long minute = horloge.millis() / 60_000;
        Fenetre fenetre = fenetres.merge(cleId, new Fenetre(minute, 1),
                (ancienne, nouvelle) -> ancienne.minute() == minute ? new Fenetre(minute, ancienne.appels() + 1) : nouvelle);
        return fenetre.appels() <= appelsParMinute;
    }

    /** Secondes à attendre avant la prochaine minute : valeur de l'en-tête Retry-After. */
    public long secondesAvantReprise() {
        return 60 - (horloge.millis() / 1000) % 60;
    }

    public int appelsParMinute() {
        return appelsParMinute;
    }
}
