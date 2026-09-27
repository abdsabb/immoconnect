package be.immoconnect.service;

import be.immoconnect.api.admin.CleApiResume;
import be.immoconnect.api.admin.RequeteCleApi;
import be.immoconnect.depot.CleApiRepository;
import be.immoconnect.entite.Administrateur;
import be.immoconnect.entite.CleApi;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cas d'utilisation « Gérer l'API RESTful et ses clés d'accès » (A7, contrainte de l'épreuve).
 * <p>
 * Une clé est tirée au hasard (256 bits) et montrée une seule fois. La base n'en conserve que
 * l'empreinte SHA-256 : qui lirait la table n'y trouverait aucune clé utilisable. Un simple hachage
 * suffit ici, sans sel ni fonction lente comme pour un mot de passe, parce qu'une clé de 256 bits
 * tirée au hasard ne se devine pas par essais successifs.
 */
@Service
public class ServiceClesApi {

    private static final String PREFIXE = "ic_";
    private static final int OCTETS = 32;
    /** La date de dernière utilisation n'est réécrite qu'une fois par minute, pas à chaque appel. */
    private static final Duration PRECISION_UTILISATION = Duration.ofMinutes(1);

    private final CleApiRepository cles;
    private final AccesAdministrateur acces;
    private final ServiceAudit audit;
    private final Clock horloge;
    private final SecureRandom hasard = new SecureRandom();

    public ServiceClesApi(CleApiRepository cles, AccesAdministrateur acces, ServiceAudit audit, Clock horloge) {
        this.cles = cles;
        this.acces = acces;
        this.audit = audit;
        this.horloge = horloge;
    }

    @Transactional(readOnly = true)
    public List<CleApiResume> lister(Integer administrateurId) {
        acces.exiger(administrateurId, AccesAdministrateur.GESTIONNAIRE);
        return cles.findAllByOrderByCreeLeDescIdDesc().stream().map(CleApiResume::depuis).toList();
    }

    @Transactional
    public CleApiResume generer(Integer administrateurId, RequeteCleApi requete, String ip) {
        Administrateur administrateur = acces.exiger(administrateurId, AccesAdministrateur.GESTIONNAIRE);
        byte[] octets = new byte[OCTETS];
        hasard.nextBytes(octets);
        String valeur = PREFIXE + HexFormat.of().formatHex(octets);
        CleApi cle = cles.save(new CleApi(administrateur, empreinte(valeur), requete.libelle().trim(), LocalDate.now(horloge)));
        audit.enregistrer(administrateur, "generation_cle_api", "cle_api#" + cle.getId(), ip);
        return CleApiResume.nouvelle(cle, valeur);
    }

    /** RA12 : la révocation est immédiate, le prochain appel avec cette clé est refusé. Elle est définitive. */
    @Transactional
    public CleApiResume revoquer(Integer administrateurId, Integer id, String ip) {
        Administrateur administrateur = acces.exiger(administrateurId, AccesAdministrateur.GESTIONNAIRE);
        CleApi cle = cles.findById(id).orElseThrow(() -> new RessourceIntrouvableException("Clé API", id));
        if (cle.isActive()) {
            cle.revoquer();
            audit.enregistrer(administrateur, "revocation_cle_api", "cle_api#" + id, ip);
        }
        return CleApiResume.depuis(cle);
    }

    /**
     * Contrôle d'une clé présentée à l'API.
     *
     * @return l'identifiant de la clé si elle existe et n'est pas révoquée
     */
    @Transactional
    public Optional<Integer> verifier(String valeur) {
        if (valeur == null || valeur.isBlank() || valeur.length() > 200) {
            return Optional.empty();
        }
        return cles.findByEmpreinte(empreinte(valeur.trim())).filter(CleApi::isActive).map(cle -> {
            LocalDateTime maintenant = LocalDateTime.now(horloge);
            LocalDateTime derniere = cle.getDerniereUtilisation();
            if (derniere == null || Duration.between(derniere, maintenant).compareTo(PRECISION_UTILISATION) >= 0) {
                cle.noterUtilisation(maintenant);
            }
            return cle.getId();
        });
    }

    static String empreinte(String valeur) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valeur.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}
