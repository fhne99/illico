# ADR-0003 : authentification par session côté serveur (modèle BFF)

- **Statut :** Acceptée
- **Date :** 2026-11-04

## Contexte

L'application Angular et l'API Spring Boot sont séparées. Les utilisateurs internes et les techniciens des prestataires accèdent à des données personnelles de locataires : l'authentification doit résister au vol de session, aux failles de script (XSS) et aux requêtes forgées (CSRF). Une authentification forte à deux facteurs est exigée (US-01).

## Options étudiées

1. **Jetons (JWT) stockés dans le navigateur** : courant, mais un jeton lisible par JavaScript peut être dérobé par une faille XSS, et il ne peut pas être révoqué avant son expiration.
2. **Jetons dans un cookie HttpOnly** : protégés du JavaScript, mais la révocation reste difficile et la gestion de leur renouvellement se complique.
3. **Session côté serveur** (modèle Backend for Frontend) : le navigateur ne reçoit qu'un cookie de session opaque ; l'état de connexion reste sur le serveur.

## Décision

Option 3. Session côté serveur, cookie `HttpOnly`, `Secure` et `SameSite=Strict`, identifiant de session renouvelé à chaque étape de la connexion, protection CSRF par jeton dans un cookie lisible par Angular et renvoyé dans l'en-tête `X-XSRF-TOKEN`.

## Conséquences

- Aucun jeton n'est jamais accessible au JavaScript du navigateur.
- La déconnexion invalide réellement la session côté serveur.
- Toute requête qui modifie des données exige le jeton CSRF.
- Plusieurs instances du back-end nécessiteraient un stockage partagé des sessions (par exemple en base) : sans objet pour le pilote, à prévoir si la charge l'exige.
- Contrôles automatiques : tests d'intégration du parcours de connexion (attributs du cookie, changement d'identifiant de session, refus sans jeton CSRF, déconnexion).
