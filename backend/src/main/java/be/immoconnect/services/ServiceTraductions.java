package be.immoconnect.services;

import be.immoconnect.dto.TraductionLigne;
import be.immoconnect.entities.Administrateur;
import be.immoconnect.entities.Langue;
import be.immoconnect.entities.Traduction;
import be.immoconnect.exceptions.DonneeInvalideException;
import be.immoconnect.exceptions.RessourceIntrouvableException;
import be.immoconnect.repositories.LangueRepository;
import be.immoconnect.repositories.TraductionRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cas d'utilisation « Gérer les langues du site » (A6, contrainte de l'épreuve : multilinguisme).
 * L'administrateur modifie les textes du site public dans chaque langue ; ils sont servis par
 * GET /traductions/{code} et prennent effet sans nouvelle livraison de l'application.
 */
@Service
public class ServiceTraductions {

    /** Une clé est un chemin en minuscules : « accueil.titre », « rdv.mes_rdv ». */
    private static final Pattern CLE = Pattern.compile("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$");
    private static final int LONGUEUR_CLE_MAX = 120;

    private final TraductionRepository traductions;
    private final LangueRepository langues;
    private final AccesAdministrateur acces;
    private final ServiceAudit audit;

    public ServiceTraductions(TraductionRepository traductions, LangueRepository langues, AccesAdministrateur acces,
                              ServiceAudit audit) {
        this.traductions = traductions;
        this.langues = langues;
        this.acces = acces;
        this.audit = audit;
    }

    /** Dictionnaire public d'une langue : les couples clé/valeur, triés par clé. */
    @Transactional(readOnly = true)
    public Map<String, String> dictionnaire(String code) {
        String codeNormalise = code.toLowerCase();
        langues.findByCode(codeNormalise).orElseThrow(() -> new RessourceIntrouvableException("Langue", code));
        return traductions.findByLangueCode(codeNormalise).stream()
                .collect(Collectors.toMap(Traduction::getCle, Traduction::getValeur, (a, b) -> a, TreeMap::new));
    }

    @Transactional(readOnly = true)
    public List<TraductionLigne> lister(Integer administrateurId) {
        acces.exiger(administrateurId, AccesAdministrateur.EDITEUR);
        Map<String, Map<String, String>> parCle = new TreeMap<>();
        for (Traduction traduction : traductions.findAllByOrderByCleAsc()) {
            parCle.computeIfAbsent(traduction.getCle(), cle -> new TreeMap<>())
                    .put(traduction.getLangue().getCode(), traduction.getValeur());
        }
        return parCle.entrySet().stream().map(e -> new TraductionLigne(e.getKey(), e.getValue())).toList();
    }

    /** Crée la clé si elle n'existe pas encore, ou met à jour les langues transmises. */
    @Transactional
    public TraductionLigne enregistrer(Integer administrateurId, String cle, TraductionLigne.Requete requete, String ip) {
        Administrateur administrateur = acces.exiger(administrateurId, AccesAdministrateur.EDITEUR);
        if (cle.length() > LONGUEUR_CLE_MAX || !CLE.matcher(cle).matches()) {
            throw new DonneeInvalideException("cle", "une clé s'écrit en minuscules, en au moins deux parties séparées par un point");
        }
        if (requete.valeurs() == null || requete.valeurs().isEmpty()) {
            throw new DonneeInvalideException("valeurs", "au moins une traduction est attendue");
        }
        for (Map.Entry<String, String> valeur : requete.valeurs().entrySet()) {
            Langue langue = langues.findByCode(valeur.getKey())
                    .orElseThrow(() -> new DonneeInvalideException("valeurs", "langue inconnue : " + valeur.getKey()));
            traductions.findByLangueCodeAndCle(langue.getCode(), cle)
                    .ifPresentOrElse(existante -> existante.setValeur(valeur.getValue().trim()),
                            () -> traductions.save(new Traduction(langue, cle, valeur.getValue().trim())));
        }
        audit.enregistrer(administrateur, "modification_traduction", tronquer("traduction#" + cle), ip);
        Map<String, String> valeurs = new LinkedHashMap<>();
        traductions.findByCle(cle).forEach(t -> valeurs.put(t.getLangue().getCode(), t.getValeur()));
        return new TraductionLigne(cle, new TreeMap<>(valeurs));
    }

    @Transactional
    public void supprimer(Integer administrateurId, String cle, String ip) {
        Administrateur administrateur = acces.exiger(administrateurId, AccesAdministrateur.EDITEUR);
        List<Traduction> lignes = traductions.findByCle(cle);
        if (lignes.isEmpty()) {
            throw new RessourceIntrouvableException("Traduction", cle);
        }
        traductions.deleteAll(lignes);
        audit.enregistrer(administrateur, "suppression_traduction", tronquer("traduction#" + cle), ip);
    }

    /** La colonne journal_audit.entite contient 60 caractères. */
    private static String tronquer(String entite) {
        return entite.length() <= 60 ? entite : entite.substring(0, 60);
    }
}
