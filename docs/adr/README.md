# Décisions d'architecture (ADR)

Chaque décision d'architecture importante est consignée dans un fichier numéroté, selon le modèle `modele.md`.

Règles :
- un ADR n'est jamais modifié une fois accepté ; si une décision change, un nouvel ADR la remplace et l'ancien passe au statut « Remplacé par ADR-XXXX » ;
- un ADR est court : contexte, options étudiées, décision, conséquences ;
- un ADR est ajouté dans la même demande de fusion que le code qui applique la décision.

| N° | Décision | Statut |
| --- | --- | --- |
| [0001](0001-architecture-en-couches-par-module.md) | Architecture en couches, organisée par module métier | Acceptée |
| [0002](0002-inversion-des-dependances-pour-les-services-externes.md) | Inversion des dépendances pour les services externes | Acceptée |
| [0003](0003-authentification-par-session-serveur.md) | Authentification par session côté serveur (modèle BFF) | Acceptée |
| [0004](0004-seaweedfs-a-la-place-de-minio.md) | SeaweedFS à la place de MinIO pour le stockage objet local | Acceptée |
| [0005](0005-journal-des-evenements-de-securite.md) | Journal des événements de sécurité en base, en ajout seul | Acceptée |
