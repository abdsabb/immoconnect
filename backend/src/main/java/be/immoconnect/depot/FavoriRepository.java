package be.immoconnect.depot;

import be.immoconnect.entite.Favori;
import be.immoconnect.entite.FavoriId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriRepository extends JpaRepository<Favori, FavoriId> {

    long deleteByIdMembreId(Integer membreId);
}
