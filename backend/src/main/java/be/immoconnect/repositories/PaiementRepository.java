package be.immoconnect.repositories;

import be.immoconnect.entities.Paiement;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaiementRepository extends JpaRepository<Paiement, Integer> {

    /** La référence Stripe est unique : elle sert de clé d'idempotence entre la réservation et le webhook. */
    @EntityGraph(attributePaths = {"rendezVous", "rendezVous.membre", "rendezVous.agent", "rendezVous.bien"})
    Optional<Paiement> findByStripePaymentIntentId(String stripePaymentIntentId);
}
