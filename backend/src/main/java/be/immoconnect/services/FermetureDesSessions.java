package be.immoconnect.services;

import be.immoconnect.entities.TypeJeton;
import be.immoconnect.repositories.JetonRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ferme toutes les sessions d'un utilisateur dans une transaction à part : la fermeture doit
 * survivre au refus qui la déclenche, alors que celui-ci annule la transaction en cours.
 */
@Service
public class FermetureDesSessions {

    private final JetonRepository jetons;
    private final Clock horloge;

    public FermetureDesSessions(JetonRepository jetons, Clock horloge) {
        this.jetons = jetons;
        this.horloge = horloge;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fermerToutes(Integer utilisateurId) {
        jetons.fermerTous(utilisateurId, TypeJeton.rafraichissement, LocalDateTime.now(horloge));
    }
}
