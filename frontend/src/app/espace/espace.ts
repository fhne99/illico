import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';

/** Espace provisoire de l'utilisateur connecté, enrichi par les prochaines user stories. */
@Component({
  selector: 'app-espace',
  templateUrl: './espace.html',
  styleUrl: './espace.scss',
})
export class Espace {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected seDeconnecter(): void {
    this.auth.deconnexion().subscribe(() => {
      void this.router.navigateByUrl('/');
    });
  }
}
