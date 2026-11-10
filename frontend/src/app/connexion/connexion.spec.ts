import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Connexion } from './connexion';

@Component({ template: '' })
class PageVide {}

describe('Page de connexion', () => {
  let fixture: ComponentFixture<Connexion>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Connexion],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([{ path: 'espace', component: PageVide }])],
    }).compileComponents();
    fixture = TestBed.createComponent(Connexion);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  const page = (): HTMLElement => fixture.nativeElement as HTMLElement;

  function saisir(selecteur: string, valeur: string): void {
    const champ = page().querySelector<HTMLInputElement>(selecteur);
    if (!champ) {
      throw new Error(`Champ introuvable : ${selecteur}`);
    }
    champ.value = valeur;
    champ.dispatchEvent(new Event('input'));
  }

  function envoyerLeFormulaire(): void {
    page().querySelector('form')?.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  async function rafraichir(): Promise<void> {
    await fixture.whenStable();
    fixture.detectChanges();
  }

  async function validerLaPremiereEtape(): Promise<void> {
    saisir('#identifiant', 'sophie.martin@logis-estuaire.fr');
    saisir('#mot-de-passe', 'un mot de passe long');
    envoyerLeFormulaire();
    http.expectOne('/api/auth/connexion').flush({ etape: 'SECOND_FACTEUR_REQUIS', message: null });
    await rafraichir();
  }

  /** RGAA 11.1 (étiquettes) et 11.13 (finalité des champs, utile aux gestionnaires de mots de passe). */
  it('associe chaque champ à une étiquette et indique sa finalité', () => {
    expect(page().querySelector('label[for="identifiant"]')).not.toBeNull();
    expect(page().querySelector('label[for="mot-de-passe"]')).not.toBeNull();
    expect(page().querySelector('#identifiant')?.getAttribute('autocomplete')).toBe('username');
    expect(page().querySelector('#mot-de-passe')?.getAttribute('autocomplete')).toBe('current-password');
  });

  it('signale les champs vides sans interroger le serveur', () => {
    envoyerLeFormulaire();

    http.expectNone('/api/auth/connexion');
    expect(page().querySelector('#identifiant')?.getAttribute('aria-invalid')).toBe('true');
    expect(page().querySelector('#identifiant')?.getAttribute('aria-describedby')).toBe('identifiant-erreur');
  });

  it('passe à la saisie du code, avec un champ adapté aux codes à usage unique', async () => {
    await validerLaPremiereEtape();

    const code = page().querySelector('#code');
    expect(page().querySelector('#mot-de-passe')).toBeNull();
    expect(code?.getAttribute('autocomplete')).toBe('one-time-code');
    expect(code?.getAttribute('inputmode')).toBe('numeric');
  });

  it('annonce un refus avec le message générique du serveur', async () => {
    saisir('#identifiant', 'sophie.martin@logis-estuaire.fr');
    saisir('#mot-de-passe', 'un mot de passe faux');
    envoyerLeFormulaire();

    http
      .expectOne('/api/auth/connexion')
      .flush({ etape: 'REFUSEE', message: 'Identifiant ou mot de passe incorrect' }, { status: 401, statusText: 'Unauthorized' });
    await rafraichir();

    expect(page().querySelector('[role="alert"]')?.textContent).toContain('Identifiant ou mot de passe incorrect');
  });

  it('ouvre l’espace de l’utilisateur après un code accepté', async () => {
    await validerLaPremiereEtape();
    saisir('#code', '123456');
    envoyerLeFormulaire();

    http.expectOne('/api/auth/second-facteur').flush({ etape: 'CONNECTE', message: null });
    http.expectOne('/api/auth/moi').flush({ identifiant: 'sophie.martin@logis-estuaire.fr', role: 'GESTIONNAIRE' });
    await rafraichir();

    expect(TestBed.inject(Router).url).toBe('/espace');
  });

  it('annonce un code refusé et vide le champ pour une nouvelle saisie', async () => {
    await validerLaPremiereEtape();
    saisir('#code', '000000');
    envoyerLeFormulaire();

    http
      .expectOne('/api/auth/second-facteur')
      .flush({ etape: 'REFUSEE', message: 'Code incorrect ou expiré' }, { status: 401, statusText: 'Unauthorized' });
    await rafraichir();

    expect(page().querySelector('[role="alert"]')?.textContent).toContain('Code incorrect ou expiré');
    expect(page().querySelector<HTMLInputElement>('#code')?.value).toBe('');
  });
});
