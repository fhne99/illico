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
     * Un mot de passe faux compte comme un échec ; un compte verrouillé refuse même le bon mot de passe.
     *
     * @return l'identifiant technique du compte si la connexion peut continuer ;
     *         vide sinon, sans distinguer les raisons du refus
     */
    @Transactional
    public Optional<UUID> verifierMotDePasse(String identifiant, String motDePasse) {
        Instant maintenant = horloge.instant();
        Optional<Utilisateur> compte = normaliser(identifiant)
            .flatMap(depot::findByIdentifiant)
            .filter(Utilisateur::peutSeConnecter);

        // Le mot de passe est toujours vérifié, même pour un compte inconnu ou verrouillé :
        // le temps de réponse ne doit révéler ni l'existence du compte, ni son verrouillage
        String empreinte = compte.map(Utilisateur::getMotDePasseHache).orElse(empreinteFactice);
        boolean motDePasseCorrect = motDePasse != null && encodeur.matches(motDePasse, empreinte);

        if (compte.isEmpty()) {
            return Optional.empty();
        }
        Utilisateur utilisateur = compte.get();
        if (utilisateur.estVerrouille(maintenant)) {
            return Optional.empty();
        }
        if (!motDePasseCorrect) {
            utilisateur.enregistrerEchec(maintenant);
            return Optional.empty();
        }
        return Optional.of(utilisateur.getId());
    }

    /**
     * Seconde étape : vérifie le code à usage unique et enregistre la connexion.
     * Un code faux compte comme un échec, au même titre qu'un mot de passe faux.
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
            .filter(utilisateur -> !utilisateur.estVerrouille(maintenant))
            .flatMap(utilisateur -> {
                OptionalLong pasAccepte = Totp.verifier(
                    Base32.decoder(utilisateur.getSecretTotp()), code, maintenant, utilisateur.getDernierPasTotp());
                if (pasAccepte.isEmpty()) {
                    utilisateur.enregistrerEchec(maintenant);
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
