import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { AuthService } from './auth.service';

/**
 * Réserve une page aux utilisateurs connectés.
 * C'est un confort d'affichage, pas une protection : l'API refuse de toute façon les requêtes non authentifiées.
 */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.estConnecte()) {
    return true;
  }
  return auth.chargerProfil().pipe(map((profil) => (profil ? true : router.createUrlTree(['/connexion']))));
};
