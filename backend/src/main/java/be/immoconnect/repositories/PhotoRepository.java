package be.immoconnect.repositories;

import be.immoconnect.entities.Photo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PhotoRepository extends JpaRepository<Photo, Integer> {

    List<Photo> findByBienIdOrderByOrdreAsc(Integer bienId);

    long countByBienId(Integer bienId);

    List<Photo> findByUrlStartingWith(String prefixe);

    /*
     * La position d'une photo est unique par bien (contrainte uq_photo_bien_ordre) : deux photos ne
     * peuvent pas échanger leur place en une seule écriture. La galerie est donc renumérotée en deux
     * temps — toutes les positions sont d'abord décalées hors de la plage utile, puis fixées une à une.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Photo p set p.ordre = p.ordre + 100 where p.bien.id = :bienId")
    int decalerPositions(Integer bienId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Photo p set p.ordre = :ordre where p.id = :id")
    int fixerPosition(Integer id, Integer ordre);
}
