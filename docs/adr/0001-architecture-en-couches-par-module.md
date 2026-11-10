# ADR-0001 : architecture en couches, organisée par module métier

- **Statut :** Acceptée
- **Date :** 2026-11-04

## Contexte

Le back-end d'Illico couvre plusieurs domaines (sécurité, référentiel, demandes, interventions, notifications) et 34 user stories. Il est développé par une petite équipe, dont un développeur junior, et devra être maintenu après le pilote. Le domaine contient quelques règles métier importantes (authentification, délais selon l'urgence, interventions en chaîne) et beaucoup de gestion de données.

Critères : lisibilité quand le code grandit, isolation et testabilité des règles métier, coût de développement, adéquation à la taille de l'équipe.

## Options étudiées

1. **Couches techniques à la racine** (`controller`, `service`, `repository`) : simple et répandue, mais une fonctionnalité se retrouve dispersée dans quatre dossiers, et rien n'empêche les domaines de dépendre les uns des autres.
2. **Couches à l'intérieur de modules métier** : chaque domaine regroupe son code ; les dépendances entre modules restent visibles et contrôlables.
3. **Architecture hexagonale stricte** (ports et adaptateurs pour tous les accès, y compris la base de données) : isolation maximale, mais une interface et une conversion de données pour chaque accès, soit un volume de code nettement supérieur pour un domaine de complexité moyenne.
4. **Microservices** : écartés dès l'étude comparative du cadrage (coût d'exploitation, complexité, impact environnemental, taille de l'équipe).

## Décision

Option 2 : un monolithe modulaire, avec dans chaque module quatre couches :

- `api` : contrôleurs REST et objets échangés avec l'application Angular ;
- `application` : cas d'usage, orchestration et transactions ;
- `domain` : entités et règles métier, sans dépendance à une technologie ;
- `infrastructure` : accès aux données et implémentations techniques.

Les dépendances vont dans un seul sens : `api` → `application` → `domain`, et `application` → `infrastructure`.

## Conséquences

- Les règles métier se testent sans base de données ni serveur, en quelques millisecondes.
- Chaque nouvelle fonctionnalité s'ajoute dans son module, sans toucher aux autres.
- Les entités du domaine portent les annotations JPA : compromis assumé, qui évite de doubler chaque entité d'un modèle de persistance.
- Contrôles automatiques (ArchUnit, à chaque build) : absence de cycle entre modules, domaine indépendant de Spring, aucun accès direct des contrôleurs aux dépôts, contrôleurs REST rangés dans les packages `api`.
