import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ReponseConnexion } from './auth.models';
import { AuthService } from './auth.service';

describe('Service de connexion', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('transmet le refus du serveur avec son message générique', () => {
    let reponse: ReponseConnexion | undefined;
    service.connexion('sophie.martin@logis-estuaire.fr', 'faux').subscribe((r) => (reponse = r));

    http
      .expectOne('/api/auth/connexion')
      .flush({ etape: 'REFUSEE', message: 'Identifiant ou mot de passe incorrect' }, { status: 401, statusText: 'Unauthorized' });

    expect(reponse).toEqual({ etape: 'REFUSEE', message: 'Identifiant ou mot de passe incorrect' });
  });

  /** Une panne ne doit pas faire croire à l'utilisateur que son mot de passe est faux. */
  it('distingue une indisponibilité du service d’un refus', () => {
    let reponse: ReponseConnexion | undefined;
    service.connexion('sophie.martin@logis-estuaire.fr', 'un mot de passe').subscribe((r) => (reponse = r));

    http.expectOne('/api/auth/connexion').flush(null, { status: 503, statusText: 'Service Unavailable' });

    expect(reponse?.etape).toBe('REFUSEE');
    expect(reponse?.message).toContain('indisponible');
  });

  it('considère l’utilisateur connecté une fois le code accepté et le profil chargé', () => {
    service.secondFacteur('123456').subscribe();

    http.expectOne('/api/auth/second-facteur').flush({ etape: 'CONNECTE', message: null });
    http.expectOne('/api/auth/moi').flush({ identifiant: 'sophie.martin@logis-estuaire.fr', role: 'GESTIONNAIRE' });

    expect(service.estConnecte()).toBe(true);
    expect(service.profil()?.identifiant).toBe('sophie.martin@logis-estuaire.fr');
  });

  it('oublie le profil à la déconnexion et récupère un nouveau jeton CSRF', () => {
    service.secondFacteur('123456').subscribe();
    http.expectOne('/api/auth/second-facteur').flush({ etape: 'CONNECTE', message: null });
    http.expectOne('/api/auth/moi').flush({ identifiant: 'sophie.martin@logis-estuaire.fr', role: 'GESTIONNAIRE' });

    service.deconnexion().subscribe();
    http.expectOne('/api/auth/deconnexion').flush(null, { status: 204, statusText: 'No Content' });
    http.expectOne('/api/auth/csrf').flush({ nomEntete: 'X-XSRF-TOKEN', valeur: 'jeton' });

    expect(service.estConnecte()).toBe(false);
  });
});
