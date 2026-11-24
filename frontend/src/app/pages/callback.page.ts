import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Component({ standalone: true, imports: [CommonModule, RouterLink], template: `<main class="callback"><div class="loader" *ngIf="state === 'loading'"></div><h1 class="serif">{{ state === 'success' ? 'You are signed in.' : state === 'error' ? 'Sign-in needs another try.' : 'Setting your table...' }}</h1><p>{{ message }}</p><a *ngIf="state !== 'loading'" routerLink="/login">{{ state === 'success' ? 'Continue to OurSociety' : 'Return to sign in' }}</a></main>`, styles: [`.callback{min-height:100vh;display:grid;place-content:center;text-align:center;gap:12px}.callback p{color:#756a66}.callback a{color:#bd2f2f;font-weight:700}.loader{width:34px;height:34px;border:3px solid #eadfd8;border-top-color:#bd2f2f;border-radius:50%;animation:spin .8s linear infinite;margin:auto}@keyframes spin{to{transform:rotate(360deg)}}`] })
export class CallbackPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  state: 'loading' | 'success' | 'error' = 'loading';
  message = 'Finishing your secure sign-in.';

  constructor() {
    if (this.route.snapshot.queryParamMap.has('error')) {
      this.fail('Google sign-in was not completed.');
      return;
    }
    this.auth.loadCurrentUser().subscribe({
      next: () => {
        this.state = 'success';
        this.message = 'Your secure session is ready.';
        setTimeout(() => this.router.navigateByUrl(
          this.auth.currentUser()?.onboardingStatus === 'ACTIVE_MEMBERSHIP' ? '/' : '/join'
        ), 700);
      },
      error: () => this.fail('We could not verify your session. Please try again.')
    });
  }

  private fail(message: string) {
    this.state = 'error';
    this.message = message;
  }
}
