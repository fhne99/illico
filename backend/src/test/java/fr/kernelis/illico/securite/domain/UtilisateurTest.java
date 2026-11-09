package fr.kernelis.illico.securite.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class UtilisateurTest {

    /** Sans normalisation, « Sophie.Martin » et « sophie.martin » donneraient deux comptes distincts. */
    @Test
    void l_identifiant_est_normalise_sans_espaces_ni_majuscules() {
        Utilisateur utilisateur = Utilisateur.creer("  Sophie.Martin@Logis-Estuaire.fr ", Role.GESTIONNAIRE);

        assertThat(utilisateur.getIdentifiant()).isEqualTo("sophie.martin@logis-estuaire.fr");
    }

    @Test
    void un_identifiant_vide_est_refuse() {
        assertThatThrownBy(() -> Utilisateur.creer("   ", Role.GESTIONNAIRE))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void un_compte_sans_role_est_refuse() {
        assertThatThrownBy(() -> Utilisateur.creer("sophie.martin@logis-estuaire.fr", null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    /** Sécurité : un compte qui vient d'être créé ne doit permettre aucune connexion avant son activation. */
    @Test
    void un_nouveau_compte_est_en_attente_d_activation_sans_mot_de_passe_ni_second_facteur() {
        Utilisateur utilisateur = Utilisateur.creer("sophie.martin@logis-estuaire.fr", Role.GESTIONNAIRE);

        assertThat(utilisateur.getStatut()).isEqualTo(StatutCompte.EN_ATTENTE_ACTIVATION);
        assertThat(utilisateur.getMotDePasseHache()).isNull();
        assertThat(utilisateur.getSecretTotp()).isNull();
        assertThat(utilisateur.getEchecsConsecutifs()).isZero();
    }
}
