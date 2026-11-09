import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';

/**
 * Ces tests protègent deux exigences d'accessibilité que l'analyse automatique axe-core
 * ne vérifie pas entièrement : la cible du lien d'évitement et sa position dans l'ordre du clavier.
 */
describe("Structure commune de l'application", () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  function afficher(): HTMLElement {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it("mène le lien d'évitement au contenu principal (RGAA 12.7)", () => {
    const page = afficher();

    const lien = page.querySelector('a.lien-evitement');
    const contenuPrincipal = page.querySelector('main');

    expect(lien?.getAttribute('href')).toBe('#contenu');
    expect(contenuPrincipal?.id).toBe('contenu');
  });

  it("place le lien d'évitement en premier dans l'ordre de navigation au clavier", () => {
    const page = afficher();

    const premierElementFocalisable = page.querySelector(
      'a[href], button, input, select, textarea, [tabindex]:not([tabindex="-1"])',
    );

    expect(premierElementFocalisable?.classList.contains('lien-evitement')).toBe(true);
  });
});
