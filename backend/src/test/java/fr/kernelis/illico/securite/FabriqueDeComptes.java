package fr.kernelis.illico.securite;

import fr.kernelis.illico.securite.domain.Role;
import fr.kernelis.illico.securite.domain.Totp;
import fr.kernelis.illico.securite.domain.Utilisateur;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Fabrique de comptes pour les tests, en attendant le parcours d'activation d'US-07. */
public final class FabriqueDeComptes {

    public static final String MOT_DE_PASSE = "un mot de passe long et memorable";

    private FabriqueDeComptes() {
    }

    public static Utilisateur compteActif(String identifiant, PasswordEncoder encodeur) {
        Utilisateur utilisateur = Utilisateur.creer(identifiant, Role.GESTIONNAIRE);
        utilisateur.activer(encodeur.encode(MOT_DE_PASSE), Totp.genererSecret());
        return utilisateur;
    }
}
