import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { Observable } from 'rxjs';
import { authGuard } from './auth.guard';

describe('Accès réservé aux utilisateurs connectés', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
  });

  function verifierAcces(): Observable<boolean | UrlTree> {
    return TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    ) as Observable<boolean | UrlTree>;
  }

  it('renvoie vers la page de connexion quand le serveur ne reconnaît aucune session', () => {
    let resultat: boolean | UrlTree | undefined;
    verifierAcces().subscribe((r) => (resultat = r));

    TestBed.inject(HttpTestingController).expectOne('/api/auth/moi').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(TestBed.inject(Router).serializeUrl(resultat as UrlTree)).toBe('/connexion');
  });

  it('laisse passer un utilisateur dont la session est reconnue', () => {
    let resultat: boolean | UrlTree | undefined;
    verifierAcces().subscribe((r) => (resultat = r));

    TestBed.inject(HttpTestingController)
      .expectOne('/api/auth/moi')
      .flush({ identifiant: 'sophie.martin@logis-estuaire.fr', role: 'GESTIONNAIRE' });

    expect(resultat).toBe(true);
  });
});
