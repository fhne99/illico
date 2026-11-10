package fr.kernelis.illico.securite.application;

import static fr.kernelis.illico.securite.domain.TypeEvenementSecurite.CODE_REFUSE;
import static fr.kernelis.illico.securite.domain.TypeEvenementSecurite.COMPTE_VERROUILLE;
import static fr.kernelis.illico.securite.domain.TypeEvenementSecurite.CONNEXION_REUSSIE;
import static fr.kernelis.illico.securite.domain.TypeEvenementSecurite.MOT_DE_PASSE_REFUSE;
import static fr.kernelis.illico.securite.domain.TypeEvenementSecurite.TENTATIVE_SUR_COMPTE_VERROUILLE;

import fr.kernelis.illico.securite.domain.Base32;
import fr.kernelis.illico.securite.domain.Totp;
import fr.kernelis.illico.securite.domain.TypeEvenementSecurite;
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
    private final JournalSecurite journal;

    /**
     * Empreinte calculée une fois au démarrage, vérifiée quand le compte n'existe pas :
     * le temps de réponse est alors le même que pour un mot de passe faux,
     * ce qui empêche de découvrir quels comptes existent en mesurant les délais.
     */
    private final String empreinteFactice;

    public ConnexionService(UtilisateurRepository depot, PasswordEncoder encodeur, Clock horloge,
                            JournalSecurite journal) {
        this.depot = depot;
        this.encodeur = encodeur;
        this.horloge = horloge;
        this.journal = journal;
        this.empreinteFactice = encodeur.encode("empreinte-factice-pour-egaliser-les-temps-de-reponse");
    }

    /**
     * Première étape de la connexion.
     * Un mot de passe faux compte comme un échec ; un compte verrouillé refuse même le bon mot de passe.
     *
     * @param adresseIp adresse de l'appelant, conservée dans le journal de sécurité
     * @return l'identifiant technique du compte si la connexion peut continuer ;
     *         vide sinon, sans distinguer les raisons du refus
     */
    @Transactional
    public Optional<UUID> verifierMotDePasse(String identifiant, String motDePasse, String adresseIp) {
        Instant maintenant = horloge.instant();
        Optional<String> identifiantNormalise = normaliser(identifiant);
        Optional<Utilisateur> compte = identifiantNormalise
            .flatMap(depot::findByIdentifiant)
            .filter(Utilisateur::peutSeConnecter);

        // Le mot de passe est toujours vérifié, même pour un compte inconnu ou verrouillé :
        // le temps de réponse ne doit révéler ni l'existence du compte, ni son verrouillage
        String empreinte = compte.map(Utilisateur::getMotDePasseHache).orElse(empreinteFactice);
        boolean motDePasseCorrect = motDePasse != null && encodeur.matches(motDePasse, empreinte);

        String saisi = identifiantNormalise.orElse(null);
        if (compte.isEmpty()) {
            journal.enregistrer(MOT_DE_PASSE_REFUSE, null, saisi, adresseIp, maintenant);
            return Optional.empty();
        }
        Utilisateur utilisateur = compte.get();
        if (utilisateur.estVerrouille(maintenant)) {
            journal.enregistrer(TENTATIVE_SUR_COMPTE_VERROUILLE, utilisateur.getId(), saisi, adresseIp, maintenant);
            return Optional.empty();
        }
        if (!motDePasseCorrect) {
            enregistrerEchec(utilisateur, MOT_DE_PASSE_REFUSE, adresseIp, maintenant);
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
    public Optional<UtilisateurConnecte> verifierSecondFacteur(EtapeMotDePasseValidee etape, String code,
                                                               String adresseIp) {
        Instant maintenant = horloge.instant();
        if (etape == null || etape.estExpiree(maintenant)) {
            journal.enregistrer(CODE_REFUSE, etape == null ? null : etape.utilisateurId(), null, adresseIp, maintenant);
            return Optional.empty();
        }
        Optional<Utilisateur> compte = depot.findById(etape.utilisateurId()).filter(Utilisateur::peutSeConnecter);
        if (compte.isEmpty()) {
            journal.enregistrer(CODE_REFUSE, etape.utilisateurId(), null, adresseIp, maintenant);
            return Optional.empty();
        }
        Utilisateur utilisateur = compte.get();
        if (utilisateur.estVerrouille(maintenant)) {
            journal.enregistrer(TENTATIVE_SUR_COMPTE_VERROUILLE, utilisateur.getId(),
                utilisateur.getIdentifiant(), adresseIp, maintenant);
            return Optional.empty();
        }
        OptionalLong pasAccepte = Totp.verifier(
            Base32.decoder(utilisateur.getSecretTotp()), code, maintenant, utilisateur.getDernierPasTotp());
        if (pasAccepte.isEmpty()) {
            enregistrerEchec(utilisateur, CODE_REFUSE, adresseIp, maintenant);
            return Optional.empty();
        }
        // Mémorise le pas du code : il ne pourra plus resservir (le verrou optimiste
        // de l'entité bloque aussi deux utilisations simultanées du même code)
        utilisateur.enregistrerConnexionReussie(pasAccepte.getAsLong(), maintenant);
        journal.enregistrer(CONNEXION_REUSSIE, utilisateur.getId(), utilisateur.getIdentifiant(), adresseIp, maintenant);
        return Optional.of(new UtilisateurConnecte(utilisateur.getId(), utilisateur.getIdentifiant(), utilisateur.getRole()));
    }

    private void enregistrerEchec(Utilisateur utilisateur, TypeEvenementSecurite type, String adresseIp, Instant maintenant) {
        journal.enregistrer(type, utilisateur.getId(), utilisateur.getIdentifiant(), adresseIp, maintenant);
        boolean vientDEtreVerrouille = utilisateur.enregistrerEchec(maintenant);
        if (vientDEtreVerrouille) {
            journal.enregistrer(COMPTE_VERROUILLE, utilisateur.getId(), utilisateur.getIdentifiant(), adresseIp, maintenant);
        }
    }

    private static Optional<String> normaliser(String identifiant) {
        try {
            return Optional.of(Utilisateur.normaliser(identifiant));
        } catch (IllegalArgumentException identifiantVide) {
            return Optional.empty();
        }
    }
}
