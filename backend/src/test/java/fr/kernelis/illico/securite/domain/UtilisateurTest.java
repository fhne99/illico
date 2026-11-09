package fr.kernelis.illico.securite.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class UtilisateurTest {

    private static final String IDENTIFIANT = "sophie.martin@logis-estuaire.fr";

    /** Sans normalisation, « Sophie.Martin » et « sophie.martin » donneraient deux comptes distincts. */
    @Test
    void l_identifiant_est_normalise_sans_espaces_ni_majuscules() {
        Utilisateur utilisateur = Utilisateur.creer("  Sophie.Martin@Logis-Estuaire.fr ", Role.GESTIONNAIRE);

        assertThat(utilisateur.getIdentifiant()).isEqualTo(IDENTIFIANT);
    }

    @Test
    void un_identifiant_vide_est_refuse() {
        assertThatThrownBy(() -> Utilisateur.creer("   ", Role.GESTIONNAIRE))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void un_compte_sans_role_est_refuse() {
        assertThatThrownBy(() -> Utilisateur.creer(IDENTIFIANT, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    /** Sécurité : un compte qui vient d'être créé ne doit permettre aucune connexion avant son activation. */
    @Test
    void un_nouveau_compte_ne_peut_pas_se_connecter_avant_son_activation() {
        Utilisateur utilisateur = Utilisateur.creer(IDENTIFIANT, Role.GESTIONNAIRE);

        assertThat(utilisateur.getStatut()).isEqualTo(StatutCompte.EN_ATTENTE_ACTIVATION);
        assertThat(utilisateur.peutSeConnecter()).isFalse();
        assertThat(utilisateur.getEchecsConsecutifs()).isZero();
    }

    @Test
    void un_compte_active_peut_se_connecter() {
        Utilisateur utilisateur = Utilisateur.creer(IDENTIFIANT, Role.GESTIONNAIRE);

        utilisateur.activer("{argon2}empreinte", "JBSWY3DPEHPK3PXP");

        assertThat(utilisateur.getStatut()).isEqualTo(StatutCompte.ACTIF);
        assertThat(utilisateur.peutSeConnecter()).isTrue();
    }

    /** L'activation fixe le mot de passe et le second facteur : la rejouer permettrait de les remplacer. */
    @Test
    void un_compte_deja_actif_ne_peut_pas_etre_reactive() {
        Utilisateur utilisateur = Utilisateur.creer(IDENTIFIANT, Role.GESTIONNAIRE);
        utilisateur.activer("{argon2}empreinte", "JBSWY3DPEHPK3PXP");

        assertThatThrownBy(() -> utilisateur.activer("{argon2}autre", "KRSXG5CTMVRXEZLU"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void l_activation_exige_un_mot_de_passe_et_un_second_facteur() {
        Utilisateur utilisateur = Utilisateur.creer(IDENTIFIANT, Role.GESTIONNAIRE);

        assertThatThrownBy(() -> utilisateur.activer("{argon2}empreinte", null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(utilisateur.peutSeConnecter()).isFalse();
    }
}
