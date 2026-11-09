package fr.kernelis.illico.securite.application;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Conservé en session entre les deux étapes de la connexion.
 * Il ne vaut PAS authentification : seul le second facteur connecte l'utilisateur.
 */
public record EtapeMotDePasseValidee(UUID utilisateurId, Instant valideeLe) implements Serializable {

    /** Délai pour saisir le code : au-delà, il faut recommencer depuis le mot de passe. */
    static final Duration DUREE_DE_VALIDITE = Duration.ofMinutes(5);

    public boolean estExpiree(Instant maintenant) {
        return maintenant.isAfter(valideeLe.plus(DUREE_DE_VALIDITE));
    }
}
