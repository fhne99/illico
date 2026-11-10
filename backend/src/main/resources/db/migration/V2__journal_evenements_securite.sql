-- US-01 : journal des événements de sécurité liés à la connexion.
-- Table en ajout seul : l'application n'y modifie ni n'y supprime jamais de ligne (voir ADR-0005).
-- Pas de clé étrangère vers utilisateur : la trace doit survivre à la suppression d'un compte.

CREATE TABLE evenement_securite (
    id                 UUID         PRIMARY KEY,
    type               VARCHAR(40)  NOT NULL,
    utilisateur_id     UUID,                   -- vide si le compte saisi n'existe pas
    identifiant_saisi  VARCHAR(254),           -- identifiant normalisé, tel que saisi
    adresse_ip         VARCHAR(45),            -- 45 caractères : longueur maximale d'une adresse IPv6
    survenu_le         TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT ck_evenement_securite_type CHECK (type IN
        ('CONNEXION_REUSSIE', 'MOT_DE_PASSE_REFUSE', 'CODE_REFUSE',
         'COMPTE_VERROUILLE', 'TENTATIVE_SUR_COMPTE_VERROUILLE'))
);

-- Recherches les plus fréquentes : l'historique d'un compte, et les événements récents
CREATE INDEX ix_evenement_securite_utilisateur ON evenement_securite (utilisateur_id, survenu_le);
CREATE INDEX ix_evenement_securite_date ON evenement_securite (survenu_le);
