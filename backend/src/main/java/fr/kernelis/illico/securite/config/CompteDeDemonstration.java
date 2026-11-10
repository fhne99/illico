package fr.kernelis.illico.securite.config;

import fr.kernelis.illico.securite.domain.Role;
import fr.kernelis.illico.securite.domain.Totp;
import fr.kernelis.illico.securite.domain.Utilisateur;
import fr.kernelis.illico.securite.infrastructure.UtilisateurRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Compte de démonstration, créé au démarrage UNIQUEMENT avec le profil « dev »,
 * en attendant le parcours de création des comptes (US-07).
 * Aucun mot de passe n'est écrit dans le code : un nouveau mot de passe aléatoire est généré à chaque démarrage
 * et affiché dans la console. Le secret du second facteur est conservé d'un démarrage à l'autre,
 * pour ne pas avoir à le ressaisir dans l'application d'authentification.
 */
@Component
@Profile("dev")
class CompteDeDemonstration implements ApplicationRunner {

    static final String IDENTIFIANT = "demo@illico.local";

    private static final Logger JOURNAL = LoggerFactory.getLogger(CompteDeDemonstration.class);

    private final UtilisateurRepository depot;
    private final PasswordEncoder encodeur;

    CompteDeDemonstration(UtilisateurRepository depot, PasswordEncoder encodeur) {
        this.depot = depot;
        this.encodeur = encodeur;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        String motDePasse = UUID.randomUUID().toString();
        Utilisateur compte = depot.findByIdentifiant(IDENTIFIANT)
            .map(existant -> {
                existant.remplacerMotDePasse(encodeur.encode(motDePasse));
                return existant;
            })
            .orElseGet(() -> {
                Utilisateur nouveau = Utilisateur.creer(IDENTIFIANT, Role.GESTIONNAIRE);
                nouveau.activer(encodeur.encode(motDePasse), Totp.genererSecret());
                return nouveau;
            });
        depot.save(compte);

        JOURNAL.warn("""

            ================ Compte de démonstration (profil dev uniquement) ================
              Identifiant  : {}
              Mot de passe : {}   (nouveau à chaque démarrage)
              Clé du second facteur, à saisir une fois dans l'application d'authentification
              (Google Authenticator, Microsoft Authenticator...), type « basée sur l'heure » :
                {}
              Adresse d'enrôlement : otpauth://totp/Illico:{}?secret={}&issuer=Illico
            ==================================================================================
            """, IDENTIFIANT, motDePasse, compte.getSecretTotp(), IDENTIFIANT, compte.getSecretTotp());
    }
}
