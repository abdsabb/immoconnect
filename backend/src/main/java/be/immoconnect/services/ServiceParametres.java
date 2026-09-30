package be.immoconnect.services;

import be.immoconnect.dto.ParametreLigne;
import be.immoconnect.dto.RequeteParametres;
import be.immoconnect.dto.SiteInfos;
import be.immoconnect.entities.Administrateur;
import be.immoconnect.entities.Parametre;
import be.immoconnect.exceptions.DonneeInvalideException;
import be.immoconnect.repositories.ParametreRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cas A5 « Configurer les paramètres du site » : nom, slogan, coordonnées et horaires de l'agence, langues
 * actives. Les clés sont fixées ici — l'administrateur règle des valeurs, il n'invente pas de paramètres —
 * et les secrets (Stripe, courriel) restent dans l'environnement du serveur, jamais en base.
 */
@Service
public class ServiceParametres {

    public static final String NOM = "agence.nom";
    public static final String SLOGAN = "agence.slogan";
    public static final String ADRESSE = "agence.adresse";
    public static final String TELEPHONE = "agence.telephone";
    public static final String EMAIL = "agence.email";
    public static final String HORAIRES = "agence.horaires";
    public static final String LANGUES = "langues.actives";

    public static final List<String> CLES = List.of(NOM, SLOGAN, ADRESSE, TELEPHONE, EMAIL, HORAIRES, LANGUES);
    /** Langues dont l'interface est livrée ; l'administrateur en active un sous-ensemble (cas A6). */
    public static final Set<String> LANGUES_DISPONIBLES = Set.of("fr", "nl", "en");
    private static final int LONGUEUR_MAX = 1000;

    private final ParametreRepository parametres;
    private final AccesAdministrateur acces;
    private final ServiceAudit audit;
    private final Clock horloge;

    public ServiceParametres(ParametreRepository parametres, AccesAdministrateur acces, ServiceAudit audit, Clock horloge) {
        this.parametres = parametres;
        this.acces = acces;
        this.audit = audit;
        this.horloge = horloge;
    }

    /** Ce que tout le site affiche : lu sans droit particulier. */
    @Transactional(readOnly = true)
    public SiteInfos site() {
        Map<String, String> v = valeurs();
        return new SiteInfos(v.get(NOM), v.get(SLOGAN), v.get(ADRESSE), v.get(TELEPHONE), v.get(EMAIL), v.get(HORAIRES),
                languesActives(v.get(LANGUES)));
    }

    @Transactional(readOnly = true)
    public List<ParametreLigne> lister(Integer administrateurId) {
        acces.exiger(administrateurId, AccesAdministrateur.GESTIONNAIRE);
        Map<String, Parametre> existants = parametres.findAll().stream().collect(Collectors.toMap(Parametre::getCle, Function.identity()));
        return CLES.stream().filter(existants::containsKey).map(cle -> ParametreLigne.depuis(existants.get(cle))).toList();
    }

    @Transactional
    public List<ParametreLigne> enregistrer(Integer administrateurId, RequeteParametres requete, String ip) {
        Administrateur administrateur = acces.exiger(administrateurId, AccesAdministrateur.GESTIONNAIRE);
        LocalDateTime maintenant = LocalDateTime.now(horloge);
        for (Map.Entry<String, String> entree : requete.valeurs().entrySet()) {
            String cle = entree.getKey();
            if (!CLES.contains(cle)) {
                throw new DonneeInvalideException(cle, "paramètre inconnu");
            }
            String valeur = normaliser(cle, entree.getValue());
            Parametre parametre = parametres.findById(cle).orElse(null);
            if (parametre == null) {
                parametres.save(new Parametre(cle, valeur, administrateur, maintenant));
            } else if (!parametre.getValeur().equals(valeur)) {
                parametre.modifier(valeur, administrateur, maintenant);
            } else {
                continue;
            }
            audit.enregistrer(administrateur, "modification_parametre", "parametre#" + cle, ip);
        }
        return lister(administrateurId);
    }

    private static String normaliser(String cle, String brut) {
        String valeur = brut == null ? "" : brut.trim();
        if (LANGUES.equals(cle)) {
            List<String> langues = Arrays.stream(valeur.toLowerCase().split("[,\\s]+")).filter(l -> !l.isBlank()).distinct().toList();
            if (langues.isEmpty() || !LANGUES_DISPONIBLES.containsAll(langues)) {
                throw new DonneeInvalideException(cle, "langues possibles : fr, nl, en ; au moins une");
            }
            if (!langues.contains("fr")) {
                throw new DonneeInvalideException(cle, "le français, langue des textes de référence, reste actif");
            }
            return String.join(",", langues);
        }
        if (valeur.isBlank()) {
            throw new DonneeInvalideException(cle, "la valeur est obligatoire");
        }
        if (valeur.length() > LONGUEUR_MAX) {
            throw new DonneeInvalideException(cle, "1 000 caractères au plus");
        }
        if (EMAIL.equals(cle) && !valeur.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new DonneeInvalideException(cle, "adresse e-mail invalide");
        }
        return valeur;
    }

    private Map<String, String> valeurs() {
        return parametres.findAll().stream().collect(Collectors.toMap(Parametre::getCle, Parametre::getValeur));
    }

    private static List<String> languesActives(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return List.of("fr", "nl", "en");
        }
        return Arrays.stream(valeur.split(",")).map(String::trim).filter(LANGUES_DISPONIBLES::contains).toList();
    }
}
