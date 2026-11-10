import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

/**
 * Contrôle automatique d'accessibilité (critères WCAG 2.1 A et AA, base du RGAA).
 * Ajouter chaque nouvel écran à la liste au fil des sprints.
 * Ce contrôle ne remplace pas l'audit manuel RGAA : il détecte environ un tiers des non-conformités.
 */
const ecrans = [
  { nom: 'accueil', chemin: '/' },
  { nom: 'connexion', chemin: '/connexion' }, // US-01
  // { nom: 'signalement', chemin: '/demandes/nouvelle' }, // US-10
];

for (const ecran of ecrans) {
  test(`l'écran ${ecran.nom} ne présente aucune non-conformité grave`, async ({ page }) => {
    await page.goto(ecran.chemin);

    const resultats = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
      .analyze();

    const graves = resultats.violations.filter((v) => v.impact === 'critical' || v.impact === 'serious');
    expect(graves, graves.map((v) => `${v.id} : ${v.help}`).join('\n')).toEqual([]);
  });
}
