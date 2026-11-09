import { Routes } from '@angular/router';
import { Accueil } from './accueil/accueil';

export const routes: Routes = [
  // RGAA 8.6 : chaque page a un titre pertinent, affiché dans l'onglet du navigateur
  { path: '', component: Accueil, title: 'Accueil – Illico' },
  { path: '**', redirectTo: '' },
];
