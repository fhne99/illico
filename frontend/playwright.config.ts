import { defineConfig, devices } from '@playwright/test';

/**
 * Tests de bout en bout et d'accessibilité.
 * En local, le serveur Angular est démarré automatiquement ;
 * en recette, définir E2E_BASE_URL pour viser l'environnement déployé.
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env['CI'],
  retries: process.env['CI'] ? 1 : 0,
  reporter: process.env['CI'] ? [['html', { open: 'never' }], ['github']] : 'list',
  use: {
    baseURL: process.env['E2E_BASE_URL'] ?? 'http://localhost:4200',
    trace: 'retain-on-failure',
  },
  projects: [
    { name: 'ordinateur', use: { ...devices['Desktop Chrome'] } },
    // Vue mobile des techniciens et des locataires
    { name: 'mobile', use: { ...devices['Pixel 7'] } },
  ],
  webServer: process.env['E2E_BASE_URL']
    ? undefined
    : { command: 'npm run start', url: 'http://localhost:4200', reuseExistingServer: !process.env['CI'] },
});
