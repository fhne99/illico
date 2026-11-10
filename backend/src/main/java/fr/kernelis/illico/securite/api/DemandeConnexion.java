package fr.kernelis.illico.securite.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corps de la requête de connexion.
 * La longueur du mot de passe est bornée : Argon2 est volontairement coûteux,
 * et un mot de passe de plusieurs mégaoctets pourrait servir à saturer le serveur.
 */
public record DemandeConnexion(
    @NotBlank @Size(max = 254) String identifiant,
    @NotBlank @Size(max = 256) String motDePasse) {
}
