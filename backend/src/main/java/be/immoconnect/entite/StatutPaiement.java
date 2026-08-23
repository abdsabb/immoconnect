package be.immoconnect.entite;

/** Statuts d'un paiement Stripe (règle RA3 : un paiement réussi ne peut plus qu'être remboursé). */
public enum StatutPaiement {
    initie, reussi, echoue, rembourse
}
