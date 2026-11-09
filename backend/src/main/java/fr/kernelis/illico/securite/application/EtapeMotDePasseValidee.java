package fr.kernelis.illico.securite.application;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Conservé en session entre les deux étapes de la connexion.
 * Il ne vaut PAS authentification : seul le second facteur, à la tâche 4, connecte l'utilisateur.
 */
public record EtapeMotDePasseValidee(UUID utilisateurId, Instant valideeLe) implements Serializable {
}
