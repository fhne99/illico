package fr.kernelis.illico.securite.application;

import fr.kernelis.illico.securite.domain.Base32;
import fr.kernelis.illico.securite.domain.Totp;
import fr.kernelis.illico.securite.domain.Utilisateur;
import fr.kernelis.illico.securite.infrastructure.UtilisateurRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConnexionService {

    private final UtilisateurRepository depot;
    private final PasswordEncoder encodeur;
    private final Clock horloge;

    /**
     * Empreinte calculée une fois au démarrage, vérifiée quand le compte n'existe pas :
     * le temps de réponse est alors le même que pour un mot de passe faux,
     * ce qui empêche de découvrir quels comptes existent en mesurant les délais.
     */
    private final String empreinteFactice;

    public ConnexionService(UtilisateurRepository depot, PasswordEncoder encodeur, Clock horloge) {
        this.depot = depot;
        this.encodeur = encodeur;
        this.horloge = horloge;
        this.empreinteFactice = encodeur.encode("empreinte-factice-pour-egaliser-les-temps-de-reponse");
    }

    /**
     * Première étape de la connexion.
     *
     * @return l'identifiant technique du compte si le mot de passe est correct pour un compte actif ;
     *         vide sinon, sans distinguer les raisons du refus
     */
    @Transactional(readOnly = true)
    public Optional<UUID> verifierMotDePasse(String identifiant, String motDePasse) {
        Optional<Utilisateur> compte = normaliser(identifiant)
            .flatMap(depot::findByIdentifiant)
            .filter(Utilisateur::peutSeConnecter);

        String empreinte = compte.map(Utilisateur::getMotDePasseHache).orElse(empreinteFactice);
        boolean motDePasseCorrect = motDePasse != null && encodeur.matches(motDePasse, empreinte);

        return compte.filter(utilisateur -> motDePasseCorrect).map(Utilisateur::getId);
    }

    /**
     * Seconde étape : vérifie le code à usage unique et enregistre la connexion.
     *
     * @param etape étape mot de passe conservée en session, ou null si elle n'a pas eu lieu
     * @return l'identité à placer en session si le code est valide ; vide sinon
     */
    @Transactional
    public Optional<UtilisateurConnecte> verifierSecondFacteur(EtapeMotDePasseValidee etape, String code) {
        Instant maintenant = horloge.instant();
        if (etape == null || etape.estExpiree(maintenant)) {
            return Optional.empty();
        }
        return depot.findById(etape.utilisateurId())
            .filter(Utilisateur::peutSeConnecter)
            .flatMap(utilisateur -> {
                OptionalLong pasAccepte = Totp.verifier(
                    Base32.decoder(utilisateur.getSecretTotp()), code, maintenant, utilisateur.getDernierPasTotp());
                if (pasAccepte.isEmpty()) {
                    return Optional.empty();
                }
                // Mémorise le pas du code : il ne pourra plus resservir (le verrou optimiste
                // de l'entité bloque aussi deux utilisations simultanées du même code)
                utilisateur.enregistrerConnexionReussie(pasAccepte.getAsLong(), maintenant);
                return Optional.of(new UtilisateurConnecte(
                    utilisateur.getId(), utilisateur.getIdentifiant(), utilisateur.getRole()));
            });
    }

    private static Optional<String> normaliser(String identifiant) {
        try {
            return Optional.of(Utilisateur.normaliser(identifiant));
        } catch (IllegalArgumentException identifiantVide) {
            return Optional.empty();
        }
    }
}
