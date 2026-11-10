import { Routes } from '@angular/router';
import { Accueil } from './accueil/accueil';
import { Connexion } from './connexion/connexion';
import { authGuard } from './core/auth/auth.guard';
import { Espace } from './espace/espace';

export const routes: Routes = [
  // RGAA 8.6 : chaque page a un titre pertinent, affiché dans l'onglet du navigateur
  { path: '', component: Accueil, title: 'Accueil – Illico' },
  { path: 'connexion', component: Connexion, title: 'Connexion – Illico' },
  { path: 'espace', component: Espace, canActivate: [authGuard], title: 'Mon espace – Illico' },
  { path: '**', redirectTo: '' },
];
