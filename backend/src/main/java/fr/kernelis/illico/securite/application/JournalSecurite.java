package fr.kernelis.illico.securite.application;

import fr.kernelis.illico.securite.domain.EvenementSecurite;
import fr.kernelis.illico.securite.domain.TypeEvenementSecurite;
import fr.kernelis.illico.securite.infrastructure.EvenementSecuriteRepository;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Enregistre les événements de sécurité à deux endroits (voir ADR-0005) :
 * en base, pour l'historique consultable par l'administrateur (US-04),
 * et dans les journaux techniques, pour la supervision et les alertes.
 */
@Component
public class JournalSecurite {

    private static final Logger JOURNAL = LoggerFactory.getLogger("illico.securite");

    private final EvenementSecuriteRepository depot;

    public JournalSecurite(EvenementSecuriteRepository depot) {
        this.depot = depot;
    }

    public void enregistrer(TypeEvenementSecurite type, UUID utilisateurId, String identifiantSaisi,
                            String adresseIp, Instant survenuLe) {
        depot.save(EvenementSecurite.creer(type, utilisateurId, identifiantSaisi, adresseIp, survenuLe));
        // Journaux techniques : ni identifiant, ni mot de passe, ni code ; seulement de quoi détecter une attaque
        if (type == TypeEvenementSecurite.CONNEXION_REUSSIE) {
            JOURNAL.info("evenement_securite type={} utilisateur={} ip={}", type, utilisateurId, adresseIp);
        } else {
            JOURNAL.warn("evenement_securite type={} utilisateur={} ip={}", type, utilisateurId, adresseIp);
        }
    }
}
