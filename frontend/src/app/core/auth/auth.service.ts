import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, firstValueFrom, from, map, of, switchMap, tap } from 'rxjs';
import { ProfilConnecte, ReponseConnexion } from './auth.models';

const MESSAGE_INDISPONIBLE = 'Le service est momentanément indisponible. Réessayez dans quelques instants.';

/**
 * Connexion en deux étapes auprès de l'API (ADR-0003).
 * Aucun jeton n'est manipulé ici : la session repose sur un cookie HttpOnly géré par le navigateur,
 * et le jeton CSRF est ajouté automatiquement par HttpClient (cookie XSRF-TOKEN, en-tête X-XSRF-TOKEN).
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly profilCourant = signal<ProfilConnecte | null>(null);

  readonly profil = this.profilCourant.asReadonly();
  readonly estConnecte = computed(() => this.profilCourant() !== null);

  /** Dépose le cookie XSRF-TOKEN. Une API indisponible ne doit pas empêcher l'application de s'afficher. */
  initialiser(): Promise<void> {
    return firstValueFrom(
      this.http.get('/api/auth/csrf').pipe(
        map(() => undefined),
        catchError(() => of(undefined)),
      ),
    );
  }

  /** Première étape : identifiant et mot de passe. */
  connexion(identifiant: string, motDePasse: string): Observable<ReponseConnexion> {
    return this.http
      .post<ReponseConnexion>('/api/auth/connexion', { identifiant, motDePasse })
      .pipe(catchError((erreur: unknown) => of(this.refus(erreur))));
  }

  /** Seconde étape : code de l'application d'authentification, puis chargement du profil. */
  secondFacteur(code: string): Observable<ReponseConnexion> {
    return this.http.post<ReponseConnexion>('/api/auth/second-facteur', { code }).pipe(
      switchMap((reponse) => this.chargerProfil().pipe(map(() => reponse))),
      catchError((erreur: unknown) => of(this.refus(erreur))),
    );
  }

  /** Interroge le serveur, seule source de vérité sur la session. */
  chargerProfil(): Observable<ProfilConnecte | null> {
    return this.http.get<ProfilConnecte>('/api/auth/moi').pipe(
      tap((profil) => this.profilCourant.set(profil)),
      catchError(() => {
        this.profilCourant.set(null);
        return of(null);
      }),
    );
  }

  /** Invalide la session côté serveur, puis récupère un nouveau jeton CSRF, effacé par la déconnexion. */
  deconnexion(): Observable<void> {
    return this.http.post<void>('/api/auth/deconnexion', {}).pipe(
      catchError(() => of(undefined)),
      tap(() => this.profilCourant.set(null)),
      switchMap(() => from(this.initialiser())),
    );
  }

  /** Un refus du serveur garde son message générique ; une panne ne doit pas passer pour un mot de passe faux. */
  private refus(erreur: unknown): ReponseConnexion {
    if (erreur instanceof HttpErrorResponse && erreur.status === 401 && erreur.error?.etape) {
      return erreur.error as ReponseConnexion;
    }
    return { etape: 'REFUSEE', message: MESSAGE_INDISPONIBLE };
  }
}
