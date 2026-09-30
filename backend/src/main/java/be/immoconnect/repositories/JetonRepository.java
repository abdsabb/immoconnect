package be.immoconnect.repositories;

import be.immoconnect.entities.Jeton;
import be.immoconnect.entities.TypeJeton;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface JetonRepository extends JpaRepository<Jeton, Integer> {

    @EntityGraph(attributePaths = {"utilisateur", "utilisateur.langue"})
    Optional<Jeton> findByEmpreinteAndType(String empreinte, TypeJeton type);

    /** Ferme tous les jetons encore ouverts d'un utilisateur : déconnexion de toutes ses sessions. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Jeton j set j.utiliseLe = :maintenant "
            + "where j.utilisateur.id = :utilisateurId and j.type = :type and j.utiliseLe is null")
    int fermerTous(Integer utilisateurId, TypeJeton type, LocalDateTime maintenant);

    /** Purge : un jeton expiré depuis longtemps ne sert plus, même de trace. */
    @Modifying
    @Query("delete from Jeton j where j.expireLe < :limite")
    int supprimerExpiresAvant(LocalDateTime limite);

    long countByUtilisateurIdAndTypeAndUtiliseLeIsNull(Integer utilisateurId, TypeJeton type);
}
