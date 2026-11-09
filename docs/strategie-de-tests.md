# Stratégie de tests d'Illico

## Principe

Un test existe pour protéger un comportement métier ou une règle de sécurité. La couverture de code est un filet de sécurité qui signale le code oublié ; elle n'est jamais un objectif en soi.

## Les règles contre les tests inutiles

1. **On teste des comportements, pas des implémentations.** Un test décrit ce que fait le système (« une demande ne se clôture pas tant qu'une intervention reste ouverte »), pas la manière dont il le fait. Un refactoring sans changement de comportement ne doit casser aucun test.
2. **Chaque critère d'acceptation a au moins un test automatisé**, écrit au niveau le plus bas possible de la pyramide.
3. **On ne teste pas :** les accesseurs, le code généré, les classes de données sans logique, la configuration du framework, ni le framework lui-même.
4. **Un test doit pouvoir échouer pour une raison identifiable.** S'il ne peut pas échouer, ou si personne ne sait dire ce qu'il protège, il est supprimé.
5. **Chaque anomalie corrigée donne un test de non-régression** qui la reproduit avant la correction.
6. **Les tests sont nommés en français et décrivent le comportement attendu**, pour être lisibles par le Product Owner et servir de documentation.
7. **Pas de simulation (mock) de ce qu'on peut utiliser pour de vrai** à coût raisonnable : la base de données et le stockage objet sont testés avec Testcontainers, pas simulés.

## La pyramide des tests

| Niveau | Ce qu'on vérifie | Outils | Quand |
| --- | --- | --- | --- |
| Unitaires back-end | Règles métier d'une classe isolée | JUnit 5, Mockito, AssertJ | Chaque demande de fusion |
| Unitaires front-end | Logique d'un composant ou d'un service | Lanceur de tests Angular, Angular Testing Library | Chaque demande de fusion |
| Architecture | Découpage en modules et en couches | ArchUnit | Chaque demande de fusion |
| Intégration | Plusieurs couches avec une vraie base | Spring Boot Test, Testcontainers | Chaque demande de fusion |
| Sécurité des autorisations | Chaque rôle et chaque cloisonnement | Spring Security Test | Chaque demande de fusion |
| Bout en bout | Parcours complets dans un navigateur, ordinateur et mobile | Playwright | Chaque demande de fusion, puis en recette |
| Accessibilité automatique | Non-conformités WCAG détectables | axe-core dans Playwright | Chaque demande de fusion |
| Sécurité dynamique | Failles courantes sur l'application déployée | OWASP ZAP | Sur l'environnement de recette |
| Performance | Tenue en charge | k6 ou Gatling | Avant la généralisation |

## Les seuils de qualité

- **JaCoCo :** 70 % des lignes du back-end couvertes, contrôle bloquant dans le pipeline.
- **SonarQube :** porte de qualité appliquée au nouveau code (couverture, duplications, failles, défauts) ; une demande de fusion qui ne la passe pas n'est pas fusionnée.
- **Exclusions de couverture :** limitées à la classe de démarrage et aux classes de données sans logique ; chaque nouvelle exclusion est justifiée ici.

## Pour aller plus loin (si le temps le permet)

- **Tests de mutation avec PIT** sur les modules du domaine : l'outil introduit volontairement des erreurs dans le code et vérifie que les tests les détectent. C'est la meilleure preuve que les tests sont utiles, et pas seulement nombreux.
