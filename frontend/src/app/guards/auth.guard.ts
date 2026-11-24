import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.currentUser()) return true;
  if (auth.initialized()) return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
  return auth.loadCurrentUser().pipe(
    map(() => true),
    catchError(() => of(router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } })))
  );
};

export const onboardingGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.currentUser()?.onboardingStatus === 'ACTIVE_MEMBERSHIP') return router.createUrlTree(['/']);
  return true;
};

export const homeGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const redirect = () => auth.currentUser()?.onboardingStatus === 'ONBOARDING_REQUIRED'
    ? router.createUrlTree(['/join'])
    : true;
  if (auth.currentUser()) return redirect();
  return auth.loadCurrentUser().pipe(
    map(() => redirect()),
    catchError(() => of(true))
  );
};
