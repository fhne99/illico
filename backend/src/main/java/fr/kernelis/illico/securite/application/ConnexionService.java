package fr.kernelis.illico.securite.application;

import fr.kernelis.illico.securite.domain.Utilisateur;
import fr.kernelis.illico.securite.infrastructure.UtilisateurRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConnexionService {

    private final UtilisateurRepository depot;
    private final PasswordEncoder encodeur;

    /**
     * Empreinte calculée une fois au démarrage, vérifiée quand le compte n'existe pas :
     * le temps de réponse est alors le même que pour un mot de passe faux,
     * ce qui empêche de découvrir quels comptes existent en mesurant les délais.
     */
    private final String empreinteFactice;

    public ConnexionService(UtilisateurRepository depot, PasswordEncoder encodeur) {
        this.depot = depot;
        this.encodeur = encodeur;
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

    private static Optional<String> normaliser(String identifiant) {
        try {
            return Optional.of(Utilisateur.normaliser(identifiant));
        } catch (IllegalArgumentException identifiantVide) {
            return Optional.empty();
        }
    }
}
