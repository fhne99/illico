package fr.kernelis.illico.securite.api;

import fr.kernelis.illico.securite.domain.Role;

/** Ce que l'application Angular apprend de l'utilisateur connecté : de quoi adapter l'affichage, rien de plus. */
public record ProfilConnecte(String identifiant, Role role) {
}
