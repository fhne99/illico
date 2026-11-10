package fr.kernelis.illico.securite.application;

import static fr.kernelis.illico.securite.ComptesDeTest.MOT_DE_PASSE;
import static fr.kernelis.illico.securite.ComptesDeTest.compteActif;
import static org.assertj.core.api.Assertions.assertThat;

import fr.kernelis.illico.TestcontainersConfiguration;
import fr.kernelis.illico.securite.domain.Role;
import fr.kernelis.illico.securite.domain.Utilisateur;
import fr.kernelis.illico.securite.infrastructure.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ConnexionServiceIntegrationTest {

    private static final String IDENTIFIANT = "sophie.martin@logis-estuaire.fr";
    private static final String IP = "203.0.113.10";

    @Autowired
    private ConnexionService service;

    @Autowired
    private UtilisateurRepository depot;

    @Autowired
    private PasswordEncoder encodeur;

    @Test
    void le_bon_mot_de_passe_d_un_compte_actif_ouvre_la_seconde_etape() {
        Utilisateur compte = depot.saveAndFlush(compteActif(IDENTIFIANT, encodeur));

        assertThat(service.verifierMotDePasse(IDENTIFIANT, MOT_DE_PASSE, IP)).hasValue(compte.getId());
    }

    @Test
    void l_identifiant_est_reconnu_quelle_que_soit_sa_casse() {
        Utilisateur compte = depot.saveAndFlush(compteActif(IDENTIFIANT, encodeur));

        assertThat(service.verifierMotDePasse("  Sophie.MARTIN@logis-estuaire.fr", MOT_DE_PASSE, IP)).hasValue(compte.getId());
    }

    @Test
    void un_mot_de_passe_faux_est_refuse() {
        depot.saveAndFlush(compteActif(IDENTIFIANT, encodeur));

        assertThat(service.verifierMotDePasse(IDENTIFIANT, "un autre mot de passe", IP)).isEmpty();
    }

    @Test
    void un_compte_inconnu_est_refuse() {
        assertThat(service.verifierMotDePasse("inconnu@logis-estuaire.fr", MOT_DE_PASSE, IP)).isEmpty();
    }

    @Test
    void un_compte_pas_encore_active_est_refuse() {
        depot.saveAndFlush(Utilisateur.creer(IDENTIFIANT, Role.GESTIONNAIRE));

        assertThat(service.verifierMotDePasse(IDENTIFIANT, MOT_DE_PASSE, IP)).isEmpty();
    }

    /** Garde-fou sur le choix de l'algorithme : un changement accidentel de configuration serait détecté. */
    @Test
    void les_mots_de_passe_sont_haches_avec_argon2id_et_jamais_stockes_en_clair() {
        Utilisateur compte = depot.saveAndFlush(compteActif(IDENTIFIANT, encodeur));

        assertThat(compte.getMotDePasseHache())
            .startsWith("{argon2}$argon2id$")
            .doesNotContain(MOT_DE_PASSE);
    }
}
