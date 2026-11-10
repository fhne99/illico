package fr.kernelis.illico.securite.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.kernelis.illico.TestcontainersConfiguration;
import fr.kernelis.illico.securite.domain.Role;
import fr.kernelis.illico.securite.domain.StatutCompte;
import fr.kernelis.illico.securite.domain.Utilisateur;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

/**
 * Vérifie la migration Flyway et le mapping de l'entité sur une vraie base PostgreSQL.
 * @Transactional : chaque test est annulé à la fin, la base reste vide pour le suivant.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class UtilisateurRepositoryIntegrationTest {

    @Autowired
    private UtilisateurRepository depot;

    @Test
    void un_compte_enregistre_est_retrouve_par_son_identifiant_avec_toutes_ses_donnees() {
        depot.saveAndFlush(Utilisateur.creer("Sophie.Martin@logis-estuaire.fr", Role.GESTIONNAIRE));

        Utilisateur retrouve = depot.findByIdentifiant("sophie.martin@logis-estuaire.fr").orElseThrow();

        assertThat(retrouve.getId()).isNotNull();
        assertThat(retrouve.getRole()).isEqualTo(Role.GESTIONNAIRE);
        assertThat(retrouve.getStatut()).isEqualTo(StatutCompte.EN_ATTENTE_ACTIVATION);
        assertThat(retrouve.getCreeLe()).isNotNull();
        assertThat(retrouve.getModifieLe()).isNotNull();
    }

    /** La contrainte d'unicité de la base protège même si une vérification applicative était oubliée. */
    @Test
    void deux_comptes_ne_peuvent_pas_avoir_le_meme_identifiant() {
        depot.saveAndFlush(Utilisateur.creer("sophie.martin@logis-estuaire.fr", Role.GESTIONNAIRE));

        assertThatThrownBy(() ->
            depot.saveAndFlush(Utilisateur.creer("SOPHIE.MARTIN@logis-estuaire.fr", Role.TECHNICIEN)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
}
