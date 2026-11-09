#!/usr/bin/env python3
"""
Importe le backlog Illico (illico_backlog.csv) dans un dépôt GitHub :
- crée les étiquettes MoSCoW, fonctions et points ;
- crée un jalon (milestone) par sprint ;
- crée un ticket par user story, avec ses critères d'acceptation en cases à cocher.

Prérequis : GitHub CLI installé et authentifié (`gh auth login`).
Pour ajouter aussi les tickets à un projet GitHub Projects, l'authentification
doit inclure le droit « project » : `gh auth refresh -s project`.

Exemples :
    python3 import_backlog.py --repo mon-compte/illico --dry-run
    python3 import_backlog.py --repo mon-compte/illico
    python3 import_backlog.py --repo mon-compte/illico --project "Illico"
"""

import argparse
import csv
import subprocess
import sys

SPRINTS = {
    "Conception": "Itération de conception (4 au 29 janvier)",
    "Sprint 1": "Sprint 1 : référentiel et accès (1er au 12 février)",
    "Sprint 2": "Sprint 2 : signaler et suivre (15 au 26 février)",
    "Sprint 3": "Sprint 3 : coordonner (1er au 12 mars)",
    "Sprint 4": "Sprint 4 : prouver et informer (15 au 26 mars)",
}

MOSCOW_COLORS = {"Must": "b60205", "Should": "fbca04", "Could": "0e8a16", "Won't": "cccccc"}
FUNCTION_COLOR = "1d76db"
POINTS_COLOR = "c5def5"


def run(cmd, dry_run, allow_fail=False):
    """Exécute une commande gh, ou l'affiche seulement en mode dry-run."""
    if dry_run:
        print("[dry-run]", " ".join(f'"{c}"' if " " in c else c for c in cmd))
        return ""
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0 and not allow_fail:
        print(f"Erreur : {' '.join(cmd)}\n{result.stderr}", file=sys.stderr)
        sys.exit(1)
    return result.stdout.strip()


def create_label(repo, name, color, dry_run):
    # --force met à jour l'étiquette si elle existe déjà
    run(["gh", "label", "create", name, "--repo", repo, "--color", color, "--force"], dry_run)


def create_milestone(repo, title, dry_run):
    # Le jalon peut déjà exister : on ignore l'erreur 422 renvoyée par l'API
    run(["gh", "api", f"repos/{repo}/milestones", "-f", f"title={title}"], dry_run, allow_fail=True)


def build_body(story):
    criteria = [line.strip() for line in story["criteres_acceptation"].splitlines() if line.strip()]
    checklist = "\n".join(f"- [ ] {c}" for c in criteria)
    return (
        f"## User story\n\n{story['user_story']}\n\n"
        f"## Critères d'acceptation\n\n{checklist}\n\n"
        f"## Informations\n\n"
        f"- Fonction : {story['fonction']}\n"
        f"- Priorité MoSCoW : {story['moscow']}\n"
        f"- Estimation : {story['points']} points\n\n"
        f"## Définition de terminé\n\n"
        f"- [ ] Code relu par un pair\n"
        f"- [ ] Tests automatisés au vert\n"
        f"- [ ] Critères d'accessibilité vérifiés\n"
        f"- [ ] Déployé en recette et validé par le Product Owner\n"
    )


def main():
    parser = argparse.ArgumentParser(description="Importe le backlog Illico dans GitHub.")
    parser.add_argument("--repo", required=True, help="Dépôt cible, au format propriétaire/nom")
    parser.add_argument("--csv", default="illico_backlog.csv", help="Chemin du fichier CSV")
    parser.add_argument("--project", help="Titre du projet GitHub Projects (facultatif)")
    parser.add_argument("--dry-run", action="store_true", help="Affiche les commandes sans les exécuter")
    args = parser.parse_args()

    with open(args.csv, newline="", encoding="utf-8") as f:
        stories = list(csv.DictReader(f))

    print(f"{len(stories)} user stories lues dans {args.csv}")

    for name, color in MOSCOW_COLORS.items():
        create_label(args.repo, name, color, args.dry_run)
    for fonction in sorted({s["fonction"].split(" ")[0] for s in stories}):
        create_label(args.repo, fonction, FUNCTION_COLOR, args.dry_run)
    for points in sorted({s["points"] for s in stories}, key=int):
        create_label(args.repo, f"{points} pts", POINTS_COLOR, args.dry_run)

    for title in SPRINTS.values():
        create_milestone(args.repo, title, args.dry_run)

    for story in stories:
        cmd = [
            "gh", "issue", "create",
            "--repo", args.repo,
            "--title", f"{story['id']} {story['titre']}",
            "--body", build_body(story),
            "--label", story["moscow"],
            "--label", story["fonction"].split(" ")[0],
            "--label", f"{story['points']} pts",
            "--milestone", SPRINTS[story["sprint"]],
        ]
        if args.project:
            cmd += ["--project", args.project]
        url = run(cmd, args.dry_run)
        print(f"{story['id']} créé {url}")

    print("Import terminé.")


if __name__ == "__main__":
    main()
