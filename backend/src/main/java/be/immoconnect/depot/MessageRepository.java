package be.immoconnect.depot;

import be.immoconnect.entite.Message;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Integer> {

    List<Message> findByMembreId(Integer membreId);
}
