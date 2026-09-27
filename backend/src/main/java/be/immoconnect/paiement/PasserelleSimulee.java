package be.immoconnect.paiement;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prestataire de paiement simulé, en mémoire : utilisé par les tests automatisés et par le
 * développement sans compte Stripe. Il reproduit le comportement utile de Stripe, cartes de test
 * comprises. Il n'est jamais actif en production, où les clés Stripe sont obligatoires.
 */
public class PasserelleSimulee implements PasserellePaiement {

    /** Mêmes numéros que les cartes de test de Stripe. */
    public static final String CARTE_ACCEPTEE = "4242424242424242";
    public static final String CARTE_REFUSEE = "4000000000000002";

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private final Map<String, IntentionPaiement> intentions = new ConcurrentHashMap<>();
    private final Set<String> remboursees = ConcurrentHashMap.newKeySet();
    private final SecureRandom hasard = new SecureRandom();

    @Override
    public String mode() {
        return "simulation";
    }

    @Override
    public String clePublique() {
        return null;
    }

    @Override
    public IntentionPaiement creerIntention(long montantCentimes, String description, String emailRecu,
                                            Map<String, String> metadonnees) {
        String id = "pi_simule_" + aleatoire(24);
        IntentionPaiement intention = new IntentionPaiement(id, id + "_secret_" + aleatoire(24), montantCentimes, "eur",
                IntentionPaiement.Statut.en_attente, Map.copyOf(metadonnees));
        intentions.put(id, intention);
        return intention;
    }

    @Override
    public IntentionPaiement consulter(String id) {
        IntentionPaiement intention = intentions.get(id);
        if (intention == null) {
            throw new IntentionIntrouvableException(id);
        }
        return intention;
    }

    /** Accepte aussi les références des données de test, qui ne sont pas nées dans cette mémoire. */
    @Override
    public void rembourser(String id) {
        remboursees.add(id);
    }

    /** Le prestataire simulé n'émet aucune notification : toute requête reçue est donc un faux. */
    @Override
    public Optional<EvenementPaiement> lireEvenement(String charge, String signature) {
        throw new SignatureInvalideException("Signature du webhook invalide");
    }

    /**
     * Joue le rôle du formulaire de carte de Stripe. Comme chez Stripe, une carte refusée laisse
     * l'intention en attente : le membre peut réessayer avec une autre carte (cas d'erreur E1).
     *
     * @return vrai si le paiement est accepté
     */
    public boolean payer(String id, String clientSecret, String numeroCarte) {
        IntentionPaiement intention = consulter(id);
        if (!intention.clientSecret().equals(clientSecret)) {
            throw new IntentionIntrouvableException(id);
        }
        if (!CARTE_ACCEPTEE.equals(numeroCarte.replace(" ", ""))) {
            return false;
        }
        intentions.put(id, new IntentionPaiement(id, clientSecret, intention.montantCentimes(), intention.devise(),
                IntentionPaiement.Statut.reussie, intention.metadonnees()));
        return true;
    }

    public boolean estRemboursee(String id) {
        return remboursees.contains(id);
    }

    private String aleatoire(int longueur) {
        StringBuilder texte = new StringBuilder(longueur);
        for (int i = 0; i < longueur; i++) {
            texte.append(ALPHABET.charAt(hasard.nextInt(ALPHABET.length())));
        }
        return texte.toString();
    }
}
