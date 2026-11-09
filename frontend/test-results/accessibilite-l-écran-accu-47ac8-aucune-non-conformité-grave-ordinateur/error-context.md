# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: accessibilite.spec.ts >> l'écran accueil ne présente aucune non-conformité grave
- Location: e2e\accessibilite.spec.ts:16:7

# Error details

```
Error: color-contrast : Elements must meet minimum color contrast ratio thresholds

expect(received).toEqual(expected) // deep equality

- Expected  -   1
+ Received  + 146

- Array []
+ Array [
+   Object {
+     "description": "Ensure the contrast between foreground and background colors meets WCAG 2 AA minimum contrast ratio thresholds",
+     "help": "Elements must meet minimum color contrast ratio thresholds",
+     "helpUrl": "https://dequeuniversity.com/rules/axe/4.14/color-contrast?application=playwright",
+     "id": "color-contrast",
+     "impact": "serious",
+     "nodes": Array [
+       Object {
+         "all": Array [],
+         "any": Array [
+           Object {
+             "data": Object {
+               "bgColor": "#faeef5",
+               "contrastRatio": 3.73,
+               "expectedContrastRatio": "4.5:1",
+               "fgColor": "#f11653",
+               "fontSize": "10.5pt (14px)",
+               "fontWeight": "normal",
+               "messageKey": null,
+             },
+             "id": "color-contrast",
+             "impact": "serious",
+             "message": "Element has insufficient color contrast of 3.73 (foreground color: #f11653, background color: #faeef5, font size: 10.5pt (14px), font weight: normal). Expected contrast ratio of 4.5:1",
+             "relatedNodes": Array [
+               Object {
+                 "html": "<a _ngcontent-ng-c1687365377=\"\" target=\"_blank\" rel=\"noopener\" class=\"pill\" href=\"https://angular.dev/tools/cli\">",
+                 "target": Array [
+                   "a[href$=\"cli\"]",
+                 ],
+               },
+               Object {
+                 "html": "<body>",
+                 "target": Array [
+                   "body",
+                 ],
+               },
+             ],
+           },
+         ],
+         "failureSummary": "Fix any of the following:
+   Element has insufficient color contrast of 3.73 (foreground color: #f11653, background color: #faeef5, font size: 10.5pt (14px), font weight: normal). Expected contrast ratio of 4.5:1",
+         "html": "<span _ngcontent-ng-c1687365377=\"\">CLI Docs</span>",
+         "impact": "serious",
+         "none": Array [],
+         "target": Array [
+           "a[href$=\"cli\"] > span",
+         ],
+       },
+       Object {
+         "all": Array [],
+         "any": Array [
+           Object {
+             "data": Object {
+               "bgColor": "#faeef5",
+               "contrastRatio": 3.73,
+               "expectedContrastRatio": "4.5:1",
+               "fgColor": "#f11653",
+               "fontSize": "10.5pt (14px)",
+               "fontWeight": "normal",
+               "messageKey": null,
+             },
+             "id": "color-contrast",
+             "impact": "serious",
+             "message": "Element has insufficient color contrast of 3.73 (foreground color: #f11653, background color: #faeef5, font size: 10.5pt (14px), font weight: normal). Expected contrast ratio of 4.5:1",
+             "relatedNodes": Array [
+               Object {
+                 "html": "<a _ngcontent-ng-c1687365377=\"\" target=\"_blank\" rel=\"noopener\" class=\"pill\" href=\"https://angular.dev/tools/language-service\">",
+                 "target": Array [
+                   ".pill:nth-child(5)",
+                 ],
+               },
+               Object {
+                 "html": "<body>",
+                 "target": Array [
+                   "body",
+                 ],
+               },
+             ],
+           },
+         ],
+         "failureSummary": "Fix any of the following:
+   Element has insufficient color contrast of 3.73 (foreground color: #f11653, background color: #faeef5, font size: 10.5pt (14px), font weight: normal). Expected contrast ratio of 4.5:1",
+         "html": "<span _ngcontent-ng-c1687365377=\"\">Angular Language Service</span>",
+         "impact": "serious",
+         "none": Array [],
+         "target": Array [
+           ".pill:nth-child(5) > span",
+         ],
+       },
+       Object {
+         "all": Array [],
+         "any": Array [
+           Object {
+             "data": Object {
+               "bgColor": "#faeef5",
+               "contrastRatio": 3.73,
+               "expectedContrastRatio": "4.5:1",
+               "fgColor": "#f11653",
+               "fontSize": "10.5pt (14px)",
+               "fontWeight": "normal",
+               "messageKey": null,
+             },
+             "id": "color-contrast",
+             "impact": "serious",
+             "message": "Element has insufficient color contrast of 3.73 (foreground color: #f11653, background color: #faeef5, font size: 10.5pt (14px), font weight: normal). Expected contrast ratio of 4.5:1",
+             "relatedNodes": Array [
+               Object {
+                 "html": "<a _ngcontent-ng-c1687365377=\"\" target=\"_blank\" rel=\"noopener\" class=\"pill\" href=\"https://angular.dev/tools/devtools\">",
+                 "target": Array [
+                   ".pill:nth-child(6)",
+                 ],
+               },
+               Object {
+                 "html": "<body>",
+                 "target": Array [
+                   "body",
+                 ],
+               },
+             ],
+           },
+         ],
+         "failureSummary": "Fix any of the following:
+   Element has insufficient color contrast of 3.73 (foreground color: #f11653, background color: #faeef5, font size: 10.5pt (14px), font weight: normal). Expected contrast ratio of 4.5:1",
+         "html": "<span _ngcontent-ng-c1687365377=\"\">Angular DevTools</span>",
+         "impact": "serious",
+         "none": Array [],
+         "target": Array [
+           ".pill:nth-child(6) > span",
+         ],
+       },
+     ],
+     "tags": Array [
+       "cat.color",
+       "wcag2aa",
+       "wcag143",
+       "TTv5",
+       "TT13.c",
+       "EN-301-549",
+       "EN-9.1.4.3",
+       "ACT",
+       "RGAAv4",
+       "RGAA-3.2.1",
+     ],
+   },
+ ]
```

# Page snapshot

```yaml
- main [ref=e3]:
  - generic [ref=e4]:
    - generic [ref=e5]:
      - heading "Hello, frontend" [level=1] [ref=e10]
      - paragraph [ref=e11]: Congratulations! Your app is running. 🎉
    - separator "Divider" [ref=e12]
    - generic [ref=e13]:
      - generic [ref=e14]:
        - link "Explore the Docs" [ref=e15] [cursor=pointer]:
          - /url: https://angular.dev
        - link "Learn with Tutorials" [ref=e19] [cursor=pointer]:
          - /url: https://angular.dev/tutorials
        - link "Prompt and best practices for AI" [ref=e23] [cursor=pointer]:
          - /url: https://angular.dev/ai/develop-with-ai
        - link "CLI Docs" [ref=e27] [cursor=pointer]:
          - /url: https://angular.dev/tools/cli
        - link "Angular Language Service" [ref=e31] [cursor=pointer]:
          - /url: https://angular.dev/tools/language-service
        - link "Angular DevTools" [ref=e35] [cursor=pointer]:
          - /url: https://angular.dev/tools/devtools
      - generic [ref=e39]:
        - link "Github" [ref=e40] [cursor=pointer]:
          - /url: https://github.com/angular/angular
        - link "X" [ref=e43] [cursor=pointer]:
          - /url: https://x.com/angular
        - link "Youtube" [ref=e46] [cursor=pointer]:
          - /url: https://www.youtube.com/channel/UCbn1OgGei-DV7aSRo_HaAiw
```

# Test source

```ts
  1  | import { test, expect } from '@playwright/test';
  2  | import AxeBuilder from '@axe-core/playwright';
  3  | 
  4  | /**
  5  |  * Contrôle automatique d'accessibilité (critères WCAG 2.1 A et AA, base du RGAA).
  6  |  * Ajouter chaque nouvel écran à la liste au fil des sprints.
  7  |  * Ce contrôle ne remplace pas l'audit manuel RGAA : il détecte environ un tiers des non-conformités.
  8  |  */
  9  | const ecrans = [
  10 |   { nom: 'accueil', chemin: '/' },
  11 |   // { nom: 'connexion', chemin: '/connexion' },          // US-01
  12 |   // { nom: 'signalement', chemin: '/demandes/nouvelle' }, // US-10
  13 | ];
  14 | 
  15 | for (const ecran of ecrans) {
  16 |   test(`l'écran ${ecran.nom} ne présente aucune non-conformité grave`, async ({ page }) => {
  17 |     await page.goto(ecran.chemin);
  18 | 
  19 |     const resultats = await new AxeBuilder({ page })
  20 |       .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
  21 |       .analyze();
  22 | 
  23 |     const graves = resultats.violations.filter((v) => v.impact === 'critical' || v.impact === 'serious');
> 24 |     expect(graves, graves.map((v) => `${v.id} : ${v.help}`).join('\n')).toEqual([]);
     |                                                                         ^ Error: color-contrast : Elements must meet minimum color contrast ratio thresholds
  25 |   });
  26 | }
  27 | 
```