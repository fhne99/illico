package fr.kernelis.illico.securite.domain;

/** Cycle de vie d'un compte : seul un compte actif peut se connecter. */
public enum StatutCompte {
    /** Créé par un administrateur, en attente de l'activation par son titulaire (US-07). */
    EN_ATTENTE_ACTIVATION,
    ACTIF,
    DESACTIVE
}
