package fr.kernelis.illico.securite.application;

import static fr.kernelis.illico.securite.ComptesDeTest.MOT_DE_PASSE;
import static fr.kernelis.illico.securite.ComptesDeTest.compteActif;
import static org.assertj.core.api.Assertions.assertThat;

import fr.kernelis.illico.TestcontainersConfiguration;
import fr.kernelis.illico.securite.domain.Base32;
import fr.kernelis.illico.securite.domain.EvenementSecurite;
import fr.kernelis.illico.securite.domain.Totp;
import fr.kernelis.illico.securite.domain.TypeEvenementSecurite;
import fr.kernelis.illico.securite.domain.Utilisateur;
import fr.kernelis.illico.securite.infrastructure.EvenementSecuriteRepository;
import fr.kernelis.illico.securite.infrastructure.UtilisateurRepository;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/** Journalisation des tentatives de connexion (critère d'US-01 et bonnes pratiques de l'OWASP). */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class JournalSecuriteIntegrationTest {

    private static final String IDENTIFIANT = "sophie.martin@logis-estuaire.fr";
    private static final String IP = "203.0.113.10";

    @Autowired
    private ConnexionService service;

    @Autowired
    private UtilisateurRepository depotUtilisateurs;

    @Autowired
    private EvenementSecuriteRepository journal;

    @Autowired
    private PasswordEncoder encodeur;

    private Utilisateur compte;

    @BeforeEach
    void creerUnCompte() {
        compte = depotUtilisateurs.saveAndFlush(compteActif(IDENTIFIANT, encodeur));
    }

    @Test
    void un_mot_de_passe_faux_est_journalise_avec_le_compte_et_l_adresse_ip() {
        service.verifierMotDePasse(IDENTIFIANT, "un mot de passe faux", IP);

        assertThat(evenementsDuCompte())
            .singleElement()
            .satisfies(evenement -> {
                assertThat(evenement.getType()).isEqualTo(TypeEvenementSecurite.MOT_DE_PASSE_REFUSE);
                assertThat(evenement.getAdresseIp()).isEqualTo(IP);
                assertThat(evenement.getSurvenuLe()).isNotNull();
            });
    }

    /** Des tentatives sur des comptes inexistants signalent une attaque par liste d'identifiants. */
    @Test
    void une_tentative_sur_un_compte_inconnu_est_journalisee_avec_l_identifiant_saisi() {
        service.verifierMotDePasse("Inconnu@Logis-Estuaire.fr", MOT_DE_PASSE, IP);

        assertThat(journal.findByIdentifiantSaisiOrderBySurvenuLeAsc("inconnu@logis-estuaire.fr"))
            .singleElement()
            .satisfies(evenement -> {
                assertThat(evenement.getType()).isEqualTo(TypeEvenementSecurite.MOT_DE_PASSE_REFUSE);
                assertThat(evenement.getUtilisateurId()).isNull();
            });
    }

    /** Critère d'US-01 : un code invalide est refusé et la tentative est journalisée. */
    @Test
    void un_code_faux_est_journalise() {
        service.verifierSecondFacteur(etapeValidee(), codeFaux(), IP);

        assertThat(typesDesEvenementsDuCompte()).containsExactly(TypeEvenementSecurite.CODE_REFUSE);
    }

    @Test
    void le_verrouillage_et_les_tentatives_qui_suivent_sont_journalises() {
        IntStream.range(0, Utilisateur.SEUIL_ECHECS)
            .forEach(i -> service.verifierMotDePasse(IDENTIFIANT, "un mot de passe faux", IP));
        service.verifierMotDePasse(IDENTIFIANT, MOT_DE_PASSE, IP);

        assertThat(typesDesEvenementsDuCompte())
            .contains(TypeEvenementSecurite.COMPTE_VERROUILLE, TypeEvenementSecurite.TENTATIVE_SUR_COMPTE_VERROUILLE)
            .filteredOn(TypeEvenementSecurite.COMPTE_VERROUILLE::equals)
            .hasSize(1);
    }

    @Test
    void une_connexion_reussie_est_journalisee() {
        service.verifierSecondFacteur(etapeValidee(), codeDuMoment(), IP);

        assertThat(typesDesEvenementsDuCompte()).containsExactly(TypeEvenementSecurite.CONNEXION_REUSSIE);
    }

    /** Garde-fou : un futur ajout au journal ne doit jamais y faire entrer un secret. */
    @Test
    void le_journal_ne_contient_jamais_le_mot_de_passe_ni_le_code() {
        String code = codeDuMoment();
        service.verifierMotDePasse(IDENTIFIANT, "mot de passe faux tape par erreur", IP);
        service.verifierSecondFacteur(etapeValidee(), code, IP);

        assertThat(evenementsDuCompte())
            .isNotEmpty()
            .allSatisfy(evenement -> assertThat(evenement.toString())
                .doesNotContain("mot de passe faux tape par erreur")
                .doesNotContain(MOT_DE_PASSE)
                .doesNotContain(code));
    }

    private List<EvenementSecurite> evenementsDuCompte() {
        return journal.findByUtilisateurIdOrderBySurvenuLeAsc(compte.getId());
    }

    private List<TypeEvenementSecurite> typesDesEvenementsDuCompte() {
        return evenementsDuCompte().stream().map(EvenementSecurite::getType).toList();
    }

    private EtapeMotDePasseValidee etapeValidee() {
        return new EtapeMotDePasseValidee(compte.getId(), Instant.now());
    }

    private String codeDuMoment() {
        return Totp.code(Base32.decoder(compte.getSecretTotp()), Totp.pas(Instant.now()), 6);
    }

    /** Un code qui n'est valide pour aucun des pas acceptés en ce moment. */
    private String codeFaux() {
        byte[] secret = Base32.decoder(compte.getSecretTotp());
        long pas = Totp.pas(Instant.now());
        List<String> valides = List.of(Totp.code(secret, pas - 1, 6), Totp.code(secret, pas, 6), Totp.code(secret, pas + 1, 6));
        return IntStream.range(0, 10)
            .mapToObj(chiffre -> String.valueOf(chiffre).repeat(6))
            .filter(code -> !valides.contains(code))
            .findFirst()
            .orElseThrow();
    }
}
