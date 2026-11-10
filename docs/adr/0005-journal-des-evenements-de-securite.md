# ADR-0005 : journal des événements de sécurité en base, en ajout seul

- **Statut :** Acceptée
- **Date :** 2026-11-10

## Contexte

US-01 exige que les tentatives de connexion refusées soient journalisées. Les bonnes pratiques (OWASP) recommandent aussi de tracer les connexions réussies et les verrouillages, sans jamais enregistrer de secret. US-04 prévoit un journal d'audit consultable par l'administrateur et non modifiable depuis l'application. Ces traces contiennent des données personnelles (identifiant, adresse IP) soumises au RGPD.

## Options étudiées

1. **Journaux techniques uniquement** (fichiers de log) : simples et exploitables par la supervision, mais difficiles à consulter depuis l'application et facilement perdus lors d'une rotation des fichiers.
2. **Table en base uniquement** : consultable et durable, mais invisible pour les outils de supervision et d'alerte.
3. **Table en base et journaux techniques** : la base pour l'historique et la consultation, les journaux pour la détection en temps réel.

## Décision

Option 3. Chaque événement est écrit dans la table `evenement_securite`, dans la même transaction que l'opération qu'il décrit, et dans les journaux techniques (catégorie `illico.securite`). La table est en ajout seul : son dépôt n'expose aucune méthode de modification ni de suppression, et ses colonnes ne sont pas modifiables.

## Conséquences

- Aucun mot de passe ni code n'est jamais journalisé ; un test le vérifie.
- Les journaux techniques ne contiennent ni identifiant ni secret : seulement le type d'événement, l'identifiant technique du compte et l'adresse IP.
- Sans clé étrangère vers les comptes, la trace survit à la suppression d'un compte.
- À prévoir : une durée de conservation conforme au RGPD, à valider avec le DPO, et une purge automatique des événements expirés ; la suppression des droits de modification et de suppression sur cette table pour le compte de base de données de l'application, en production.
- Derrière un proxy inverse, l'adresse IP doit être lue dans les en-têtes transmis par le proxy (configuration `server.forward-headers-strategy`), sous peine de n'enregistrer que l'adresse du proxy.
