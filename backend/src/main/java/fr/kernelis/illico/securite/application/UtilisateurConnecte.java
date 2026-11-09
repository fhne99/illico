package fr.kernelis.illico.securite.application;

import fr.kernelis.illico.securite.domain.Role;
import java.io.Serializable;
import java.util.UUID;

/**
 * Identité de l'utilisateur connecté, conservée dans la session côté serveur.
 * Ne contient aucune donnée secrète : ni mot de passe, ni secret du second facteur.
 */
public record UtilisateurConnecte(UUID id, String identifiant, Role role) implements Serializable {
}
