package fr.kernelis.illico.securite.domain;

/** Rôles d'Illico : chacun n'accède qu'aux fonctions de son métier (US-02). */
public enum Role {
    LOCATAIRE,
    GESTIONNAIRE,
    RESPONSABLE_RESIDENCE,
    TECHNICIEN,
    ADMINISTRATEUR
}
