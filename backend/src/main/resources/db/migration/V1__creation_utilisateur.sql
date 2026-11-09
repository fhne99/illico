-- US-01 : comptes utilisateurs et données nécessaires à l'authentification forte.
-- Les rattachements à une résidence ou à un prestataire viendront avec US-03.

CREATE TABLE utilisateur (
    id                  UUID         PRIMARY KEY,
    identifiant         VARCHAR(254) NOT NULL,
    role                VARCHAR(30)  NOT NULL,
    statut              VARCHAR(30)  NOT NULL,
    mot_de_passe_hache  VARCHAR(255),          -- vide tant que le compte n'est pas activé (US-07)
    secret_totp         VARCHAR(255),          -- secret du second facteur, renseigné à l'activation
    dernier_pas_totp    BIGINT,                -- dernier code accepté : empêche sa réutilisation
    echecs_consecutifs  INTEGER      NOT NULL DEFAULT 0,
    verrouille_jusqua   TIMESTAMP WITH TIME ZONE,
    derniere_connexion  TIMESTAMP WITH TIME ZONE,
    cree_le             TIMESTAMP WITH TIME ZONE NOT NULL,
    modifie_le          TIMESTAMP WITH TIME ZONE NOT NULL,
    version             BIGINT       NOT NULL DEFAULT 0,  -- verrou optimiste : deux échecs simultanés ne s'écrasent pas

    CONSTRAINT uk_utilisateur_identifiant UNIQUE (identifiant),
    -- Défense en profondeur : l'identifiant est normalisé par l'application, la base le garantit aussi
    CONSTRAINT ck_utilisateur_identifiant_minuscules CHECK (identifiant = lower(identifiant)),
    CONSTRAINT ck_utilisateur_role CHECK (role IN
        ('LOCATAIRE', 'GESTIONNAIRE', 'RESPONSABLE_RESIDENCE', 'TECHNICIEN', 'ADMINISTRATEUR')),
    CONSTRAINT ck_utilisateur_statut CHECK (statut IN ('EN_ATTENTE_ACTIVATION', 'ACTIF', 'DESACTIVE')),
    CONSTRAINT ck_utilisateur_echecs_positifs CHECK (echecs_consecutifs >= 0)
);
