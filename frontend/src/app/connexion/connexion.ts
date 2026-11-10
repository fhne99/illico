import { Component, ElementRef, Injector, Signal, afterNextRender, inject, signal, viewChild } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';

type Etape = 'mot-de-passe' | 'code';

/**
 * Page de connexion en deux étapes.
 * Accessibilité : éléments HTML natifs, étiquettes associées, finalité des champs (autocomplete),
 * erreurs annoncées et reliées à leur champ, focus déplacé à chaque changement d'étape.
 */
@Component({
  selector: 'app-connexion',
  imports: [ReactiveFormsModule],
  templateUrl: './connexion.html',
  styleUrl: './connexion.scss',
})
export class Connexion {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly injector = inject(Injector);

  protected readonly etape = signal<Etape>('mot-de-passe');
  protected readonly erreur = signal<string | null>(null);
  protected readonly motDePasseVisible = signal(false);
  private readonly envoiEnCours = signal(false);

  private readonly titreCode = viewChild<ElementRef<HTMLElement>>('titreCode');
  private readonly resumeErreur = viewChild<ElementRef<HTMLElement>>('resumeErreur');

  protected readonly identifiants = new FormGroup({
    identifiant: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    motDePasse: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  protected readonly verification = new FormGroup({
    code: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.pattern(/^\d{6}$/)] }),
  });

  protected champInvalide(formulaire: FormGroup, nom: string): boolean {
    const champ = formulaire.get(nom);
    return !!champ && champ.invalid && (champ.touched || champ.dirty);
  }

  protected envoyerIdentifiants(): void {
    if (this.identifiants.invalid) {
      this.identifiants.markAllAsTouched();
      return;
    }
    if (this.envoiEnCours()) {
      return;
    }
    const { identifiant, motDePasse } = this.identifiants.getRawValue();
    this.envoiEnCours.set(true);
    this.erreur.set(null);
    this.auth.connexion(identifiant, motDePasse).subscribe((reponse) => {
      this.envoiEnCours.set(false);
      // Le mot de passe n'est pas conservé dans le formulaire plus longtemps que nécessaire
      this.identifiants.controls.motDePasse.reset();
      if (reponse.etape === 'SECOND_FACTEUR_REQUIS') {
        this.etape.set('code');
        this.deplacerFocus(this.titreCode);
      } else {
        this.signalerErreur(reponse.message);
      }
    });
  }

  protected envoyerCode(): void {
    if (this.verification.invalid) {
      this.verification.markAllAsTouched();
      return;
    }
    if (this.envoiEnCours()) {
      return;
    }
    this.envoiEnCours.set(true);
    this.erreur.set(null);
    this.auth.secondFacteur(this.verification.getRawValue().code).subscribe((reponse) => {
      this.envoiEnCours.set(false);
      if (reponse.etape === 'CONNECTE') {
        void this.router.navigateByUrl('/espace');
      } else {
        this.verification.reset();
        this.signalerErreur(reponse.message);
      }
    });
  }

  /** Le code a expiré ou l'utilisateur s'est trompé de compte : retour à la première étape. */
  protected recommencer(): void {
    this.etape.set('mot-de-passe');
    this.erreur.set(null);
    this.verification.reset();
  }

  private signalerErreur(message: string | null): void {
    this.erreur.set(message ?? 'La connexion a échoué.');
    this.deplacerFocus(this.resumeErreur);
  }

  /** Après l'affichage, le focus va au nouvel élément : un utilisateur de lecteur d'écran sait où il en est. */
  private deplacerFocus(cible: Signal<ElementRef<HTMLElement> | undefined>): void {
    afterNextRender(() => cible()?.nativeElement.focus(), { injector: this.injector });
  }
}
