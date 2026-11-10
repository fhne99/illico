# ADR-0004 : SeaweedFS à la place de MinIO pour le stockage objet local

- **Statut :** Acceptée
- **Date :** 2026-11-04

## Contexte

L'environnement de développement local prévoyait MinIO pour simuler le stockage objet compatible S3 de la production. Au lancement de `docker compose`, l'image `minio/minio` s'est révélée introuvable : MinIO a cessé de publier ses images Docker en octobre 2025 et son dépôt a été archivé en avril 2026.

Critères : compatibilité avec l'API S3, maturité, maintenance active, simplicité de configuration.

## Options étudiées

1. **Une ancienne image MinIO** : plus maintenue, donc exposée aux failles non corrigées.
2. **RustFS** : compatible S3, mais encore en version candidate.
3. **Garage** : maintenu, mais demande un fichier de configuration dédié.
4. **SeaweedFS** : projet maintenu depuis 2012, API S3 activée par une simple option de démarrage.

## Décision

Option 4 : SeaweedFS (`chrislusf/seaweedfs`, commande `server -s3`, port 8333).

## Conséquences

- L'environnement local démarre sans configuration supplémentaire.
- Le code ne dépend que de l'API S3, pas de SeaweedFS : le fournisseur de production reste libre (voir ADR-0002).
- La veille technologique surveille désormais aussi la pérennité des outils de développement.
