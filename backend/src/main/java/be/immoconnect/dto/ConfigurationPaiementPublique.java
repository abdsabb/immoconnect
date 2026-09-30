package be.immoconnect.dto;

import java.math.BigDecimal;

/** Réglages de paiement lisibles par le navigateur : aucun secret, la clé publiable est faite pour être exposée. */
public record ConfigurationPaiementPublique(String mode, String clePublique, BigDecimal prixCreneauPremium) {
}
