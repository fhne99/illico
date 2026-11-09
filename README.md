# Illico

GMAO légère pour bailleur social : centralise les demandes d'intervention quel que soit leur canal et les suit jusqu'à leur résolution complète.

## Structure du dépôt

```
illico/
├── backend/            API Spring Boot (à générer, voir étape 2)
├── frontend/           Application Angular installable (à générer, voir étape 3)
├── backend-ajouts/     Fichiers à copier dans backend/ après génération
├── frontend-ajouts/    Fichiers à copier dans frontend/ après génération
├── docs/               Stratégie de tests et documentation
├── .github/            Pipeline CI et modèle de demande de fusion
├── docker-compose.yml  PostgreSQL, stockage objet, mails, SonarQube
└── renovate.json       Mises à jour automatiques des dépendances
```

## Prérequis

Java (dernière version LTS), Node.js (version LTS compatible avec Angular), Docker, Git, GitHub CLI.

## Démarrage

### 1. Lancer l'environnement local

```bash
docker compose up -d
```

PostgreSQL sur le port 5432, stockage objet sur http://localhost:9001, mails capturés sur http://localhost:8025.

### 2. Générer le back-end

Sur https://start.spring.io : projet Maven, langage Java, dernière version stable de Spring Boot, groupe `fr.kernelis`, artefact `illico`, package `fr.kernelis.illico`, Java en dernière version LTS, configuration YAML.

Dépendances : Spring Web, Spring Security, Spring Data JPA, PostgreSQL Driver, Flyway Migration, Validation, Spring Boot Actuator, Java Mail Sender, Testcontainers.

Décompresser le projet dans `backend/`, puis :

1. Supprimer `src/main/resources/application.properties` s'il existe.
2. Copier le contenu de `backend-ajouts/src/` dans `backend/src/`.
3. Reporter le contenu de `backend-ajouts/pom-ajouts.xml` dans `backend/pom.xml`.
4. Dans la classe `TestcontainersConfiguration` générée, fixer l'image PostgreSQL à `postgres:17-alpine`, comme dans `docker-compose.yml`.
5. Vérifier : `cd backend && ./mvnw verify` (Docker doit être lancé).

### 3. Générer le front-end

```bash
npx @angular/cli@latest new frontend --routing --style=scss --ssr=false
cd frontend
npx ng add @angular/pwa
npx ng add @angular/material
npx ng add @angular-eslint/schematics
npm install --save-dev @playwright/test @axe-core/playwright
```

Puis :

1. Copier le contenu de `frontend-ajouts/` dans `frontend-ajouts`.
2. Ajouter dans `package.json` un script `test:ci` qui lance les tests une fois, avec couverture, sans mode surveillance (la commande exacte dépend du lanceur de tests installé par Angular).
3. Vérifier le chemin du rapport de couverture dans `sonar-project.properties`.
4. Vérifier : `npm run lint`, `npm run test:ci`, `npx playwright test`.

### 4. SonarQube en local

```bash
docker compose --profile qualite up -d sonarqube
```

Ouvrir http://localhost:9002, créer les projets `illico-backend` et `illico-frontend`, générer un jeton, puis :

```bash
cd backend && ./mvnw verify sonar:sonar -Dsonar.host.url=http://localhost:9002 -Dsonar.token=<jeton>
cd frontend && npx sonarqube-scanner -Dsonar.host.url=http://localhost:9002 -Dsonar.token=<jeton>
```

Sous Linux, SonarQube peut exiger `sudo sysctl -w vm.max_map_count=524288`.

### 5. SonarQube dans le pipeline

Le SonarQube local n'est pas joignable depuis GitHub. Dans le pipeline, l'analyse vise SonarQube Cloud (gratuit pour un dépôt public) : créer l'organisation et les deux projets, puis déclarer dans le dépôt GitHub le secret `SONAR_TOKEN` et les variables `SONAR_HOST_URL` (`https://sonarcloud.io`) et `SONAR_ORGANIZATION`. Sans jeton, l'étape d'analyse est ignorée et le reste du pipeline s'exécute.

### 6. Importer le backlog

```bash
python3 import_backlog.py --repo <compte>/illico --dry-run
python3 import_backlog.py --repo <compte>/illico --project "Illico"
```

### 7. Activer Renovate

Installer l'application Renovate sur le dépôt GitHub ; elle lit `renovate.json` et propose chaque lundi les mises à jour, en ignorant les versions publiées depuis moins de 14 jours.

## Qualité et tests

Voir [docs/strategie-de-tests.md](docs/strategie-de-tests.md).
