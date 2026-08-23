package be.immoconnect.depot;

import be.immoconnect.entite.JournalAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalAuditRepository extends JpaRepository<JournalAudit, Integer> {
}
