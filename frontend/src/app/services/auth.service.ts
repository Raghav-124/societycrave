import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, finalize, firstValueFrom, of, shareReplay, tap, throwError } from 'rxjs';

export interface CurrentUser {
  id: string;
  email: string;
  displayName: string | null;
  onboardingStatus: 'ONBOARDING_REQUIRED' | 'ACTIVE_MEMBERSHIP';
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  readonly currentUser = signal<CurrentUser | null>(null);
  readonly loading = signal(true);
  readonly initialized = signal(false);
  private loadRequest$: Observable<CurrentUser> | null = null;

  initialize(): Promise<CurrentUser | null> {
    return firstValueFrom(this.loadCurrentUser().pipe(catchError(() => of(null))));
  }

  loadCurrentUser(): Observable<CurrentUser> {
    if (this.loadRequest$) return this.loadRequest$;

    this.loading.set(true);
    this.loadRequest$ = this.http.get<CurrentUser>('/api/auth/me', { withCredentials: true }).pipe(
      tap(user => this.currentUser.set(user)),
      catchError(error => {
        this.currentUser.set(null);
        return throwError(() => error);
      }),
      finalize(() => {
        this.loading.set(false);
        this.initialized.set(true);
        this.loadRequest$ = null;
      }),
      shareReplay({ bufferSize: 1, refCount: false })
    );
    return this.loadRequest$;
  }

  logout(): Observable<void> {
    return this.http.post<void>('/api/auth/logout', {}, { withCredentials: true }).pipe(
      tap(() => this.currentUser.set(null))
    );
  }

  startGoogleSignIn(): void {
    window.location.assign('/oauth2/authorization/google');
  }
}
