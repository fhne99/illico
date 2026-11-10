package fr.kernelis.illico.securite.domain;

public enum TypeEvenementSecurite {
    CONNEXION_REUSSIE,
    /** Mot de passe faux, ou compte inconnu ou inactif : les cas ne sont pas distingués pour l'utilisateur. */
    MOT_DE_PASSE_REFUSE,
    /** Code faux, déjà utilisé, ou première étape absente ou expirée. */
    CODE_REFUSE,
    /** Le cinquième échec consécutif vient de verrouiller le compte. */
    COMPTE_VERROUILLE,
    /** Tentative pendant un verrouillage : signe possible d'une attaque en cours. */
    TENTATIVE_SUR_COMPTE_VERROUILLE
}
