# ADR-0002 : inversion des dépendances pour les services externes

- **Statut :** Acceptée
- **Date :** 2026-11-10

## Contexte

Illico s'appuie sur des services extérieurs à l'application : stockage objet des photos et des PDF, envoi des mails, import du fichier CSV du logiciel de gestion locative, et à terme export vers le logiciel comptable. Ces services peuvent changer de fournisseur ou de format : l'image MinIO, prévue pour le stockage local, a cessé d'être distribuée pendant la mise en place du projet (voir ADR-0004).

La base PostgreSQL, en revanche, est un choix stable du projet, utilisé à travers Spring Data JPA.

## Options étudiées

1. **Appeler directement les bibliothèques des services** depuis les cas d'usage : le moins de code, mais un changement de fournisseur oblige à modifier le code métier, et les tests doivent démarrer chaque service.
2. **Passer par une interface pour tous les accès**, base de données comprise (architecture hexagonale stricte) : voir ADR-0001, coût jugé disproportionné pour la base de données.
3. **Passer par une interface uniquement pour les services externes**, en appliquant le principe d'inversion des dépendances (le « D » de SOLID).

## Décision

Option 3. Pour chaque service externe, la couche `application` définit une interface décrivant le besoin en termes métier (par exemple `StockageFichiers`), et la couche `infrastructure` en fournit l'implémentation technique (par exemple `StockageS3`). La base de données reste utilisée directement à travers les dépôts Spring Data.

## Conséquences

- Remplacer un fournisseur ne modifie qu'une classe de l'infrastructure.
- Les tests des cas d'usage utilisent une implémentation en mémoire, sans conteneur ; l'implémentation réelle est testée séparément avec Testcontainers.
- Le nom et les méthodes de chaque interface parlent le langage du métier, pas celui de la technologie.
- Contrôle automatique à ajouter avec le premier service externe : les implémentations des services externes résident dans `infrastructure`, et la couche `application` ne dépend d'aucune bibliothèque cliente de ces services.
