package be.immoconnect.service;

import be.immoconnect.api.message.ConversationResume;
import be.immoconnect.api.message.MessageResume;
import be.immoconnect.api.message.RequeteMessage;
import be.immoconnect.depot.AgentImmobilierRepository;
import be.immoconnect.depot.MembreRepository;
import be.immoconnect.depot.MessageRepository;
import be.immoconnect.entite.AgentImmobilier;
import be.immoconnect.entite.Membre;
import be.immoconnect.entite.Message;
import be.immoconnect.entite.Message.Expediteur;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cas d'utilisation « Envoyer un message à un agent immobilier » (M3) et « Répondre aux messages
 * des membres » (AG4). Une conversation est le couple (membre, agent) ; l'utilisateur connecté en
 * est toujours l'une des deux parties, ce qui l'empêche de lire ou d'écrire dans celle d'un autre.
 */
@Service
public class ServiceMessagerie {

    private static final int LONGUEUR_APERCU = 120;

    private final MessageRepository messages;
    private final MembreRepository membres;
    private final AgentImmobilierRepository agents;
    private final ServiceAudit audit;
    private final Clock horloge;

    public ServiceMessagerie(MessageRepository messages, MembreRepository membres, AgentImmobilierRepository agents,
                             ServiceAudit audit, Clock horloge) {
        this.messages = messages;
        this.membres = membres;
        this.agents = agents;
        this.audit = audit;
        this.horloge = horloge;
    }

    /** Les conversations de l'utilisateur connecté, la plus récemment active en premier. */
    @Transactional(readOnly = true)
    public List<ConversationResume> conversations(Integer utilisateurId, String role) {
        Expediteur moi = partie(role);
        List<Message> tous = moi == Expediteur.membre
                ? messages.findByMembreIdOrderByEnvoyeLeDescIdDesc(utilisateurId)
                : messages.findByAgentIdOrderByEnvoyeLeDescIdDesc(utilisateurId);

        // Les messages arrivent du plus récent au plus ancien : le premier de chaque interlocuteur est le dernier échangé
        Map<Integer, List<Message>> parInterlocuteur = new LinkedHashMap<>();
        for (Message message : tous) {
            Integer interlocuteurId = moi == Expediteur.membre ? message.getAgent().getId() : message.getMembre().getId();
            parInterlocuteur.computeIfAbsent(interlocuteurId, id -> new ArrayList<>()).add(message);
        }
        return parInterlocuteur.entrySet().stream().map(conversation -> {
            Message dernier = conversation.getValue().getFirst();
            long nonLus = conversation.getValue().stream().filter(m -> m.getExpediteur() != moi && !m.isLu()).count();
            String interlocuteur = (moi == Expediteur.membre ? dernier.getAgent() : dernier.getMembre()).getNomComplet();
            return new ConversationResume(conversation.getKey(), interlocuteur, apercu(dernier.getContenu()),
                    dernier.getEnvoyeLe(), dernier.getExpediteur() == moi, nonLus);
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<MessageResume> conversation(Integer utilisateurId, String role, Integer interlocuteurId) {
        Expediteur moi = partie(role);
        Integer membreId = moi == Expediteur.membre ? utilisateurId : interlocuteurId;
        Integer agentId = moi == Expediteur.membre ? interlocuteurId : utilisateurId;
        return messages.findByMembreIdAndAgentIdOrderByEnvoyeLeAscIdAsc(membreId, agentId).stream()
                .map(message -> MessageResume.depuis(message, utilisateurId))
                .toList();
    }

    /**
     * Un membre écrit à l'agent de son choix. Un agent, lui, ne fait que répondre : il ne peut pas
     * démarcher un membre qui ne lui a jamais écrit.
     */
    @Transactional
    public MessageResume envoyer(Integer utilisateurId, String role, RequeteMessage requete, String ip) {
        Expediteur moi = partie(role);
        Integer membreId = moi == Expediteur.membre ? utilisateurId : requete.destinataireId();
        Integer agentId = moi == Expediteur.membre ? requete.destinataireId() : utilisateurId;

        Membre membre = membres.findById(membreId).orElseThrow(() -> new RessourceIntrouvableException("Membre", membreId));
        AgentImmobilier agent = agents.findById(agentId).orElseThrow(() -> new RessourceIntrouvableException("Agent", agentId));
        if (moi == Expediteur.agent && !messages.existsByMembreIdAndAgentId(membreId, agentId)) {
            throw new OperationInterditeException("Un agent ne peut que répondre à un membre qui lui a écrit");
        }
        Message message = messages.save(new Message(membre, agent, moi, requete.contenu().trim(), LocalDateTime.now(horloge)));
        audit.enregistrer(message.getAuteur(), "envoi_message", "message#" + message.getId(), ip);
        return MessageResume.depuis(message, utilisateurId);
    }

    /** Ouvrir une conversation marque comme lus les messages reçus — jamais ceux que l'on a écrits. */
    @Transactional
    public void marquerLue(Integer utilisateurId, String role, Integer interlocuteurId) {
        Expediteur moi = partie(role);
        if (moi == Expediteur.membre) {
            messages.marquerLus(utilisateurId, interlocuteurId, Expediteur.agent);
        } else {
            messages.marquerLus(interlocuteurId, utilisateurId, Expediteur.membre);
        }
    }

    private static Expediteur partie(String role) {
        return switch (role) {
            case "membre" -> Expediteur.membre;
            case "agent" -> Expediteur.agent;
            default -> throw new OperationInterditeException("La messagerie est réservée aux membres et aux agents");
        };
    }

    private static String apercu(String contenu) {
        return contenu.length() <= LONGUEUR_APERCU ? contenu : contenu.substring(0, LONGUEUR_APERCU - 1) + "…";
    }
}
