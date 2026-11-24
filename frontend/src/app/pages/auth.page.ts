import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../services/auth.service';

type AuthMode = 'login' | 'signup';

@Component({
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <main class="auth-page">
      <section class="auth-visual" aria-label="SocietyCrave homemade food">
        <div class="visual-shade"></div>
        <a routerLink="/" class="visual-brand" aria-label="SocietyCrave home">
          <span class="brand-mark">SC</span>
          <span>SocietyCrave</span>
        </a>
        <div class="visual-copy">
          <p class="eyebrow">Good food, close to home</p>
          <h1 class="serif">Ghar ka swaad,<br><em>ab apni society mein.</em></h1>
          <p>Discover the recipes and people that make your neighbourhood feel like home.</p>
        </div>
        <div class="food-scene" aria-hidden="true">
          <div class="glow"></div>
          <div class="plate plate-large"><span>🍛</span></div>
          <div class="plate plate-small"><span>🫓</span></div>
          <div class="food-note"><strong>Made with love</strong><small>By home chefs near you</small></div>
        </div>
      </section>

      <section class="auth-panel">
        <a routerLink="/" class="mobile-brand" aria-label="SocietyCrave home">
          <span class="brand-mark">SC</span> SocietyCrave
        </a>
        <div class="auth-content">
          <p class="eyebrow">{{ mode === 'login' ? 'Welcome back' : 'Join the community' }}</p>
          <h2 class="serif">{{ mode === 'login' ? 'Welcome back!' : 'Create your account.' }}</h2>
          <p class="supporting-text">
            {{ mode === 'login'
              ? 'Sign in to discover homemade food in your society.'
              : 'Join your society’s homemade food community. After Google authentication, choose your society and set up your membership.' }}
          </p>

          <div class="oauth-error" *ngIf="oauthError" role="alert">
            <span class="error-icon">!</span>
            <span>{{ oauthError }}</span>
          </div>

          <button
            type="button"
            class="google-button"
            [disabled]="isSigningIn()"
            (click)="googleSignIn()"
            aria-label="Continue with Google">
            <span class="google-icon" aria-hidden="true">G</span>
            <span>{{ isSigningIn() ? 'Connecting to Google…' : 'Continue with Google' }}</span>
            <span class="arrow" aria-hidden="true">{{ isSigningIn() ? '…' : '→' }}</span>
          </button>

          <div class="trust-note">
            <span class="lock-icon" aria-hidden="true">⌁</span>
            <span>Secure sign-in powered by Google. No password required.</span>
          </div>

          <p class="switch-copy">
            {{ mode === 'login' ? 'New to SocietyCrave?' : 'Already have an account?' }}
            <a [routerLink]="mode === 'login' ? '/signup' : '/login'">
              {{ mode === 'login' ? 'Create an account' : 'Login' }}
            </a>
          </p>
        </div>
        <p class="legal-copy">
          By continuing, you agree to SocietyCrave’s <a href="#">Terms</a> and <a href="#">Privacy Policy</a>.
        </p>
      </section>
    </main>
  `,
  styles: [`
    :host { display: block; min-height: 100vh; }
    .auth-page { min-height: 100vh; display: grid; grid-template-columns: minmax(420px, 1fr) minmax(420px, .9fr); background: #fff; }
    .auth-visual { position: relative; overflow: hidden; min-height: 100vh; padding: 38px clamp(32px, 6vw, 88px); color: #fff; background: linear-gradient(145deg, #7d171e 0%, #ad292a 48%, #d86145 100%); }
    .visual-shade { position: absolute; inset: 0; background: radial-gradient(circle at 75% 76%, #f5bb7c55, transparent 32%), linear-gradient(180deg, #4f111a55, transparent 48%); }
    .visual-brand, .visual-copy, .food-scene { position: relative; z-index: 1; }
    .visual-brand, .mobile-brand { display: inline-flex; align-items: center; gap: 10px; font-size: 1.15rem; font-weight: 800; letter-spacing: -.02em; }
    .brand-mark { display: grid; place-items: center; width: 35px; height: 35px; border-radius: 10px; background: #fff; color: var(--red-dark); font-size: .68rem; letter-spacing: -.08em; }
    .visual-copy { max-width: 500px; margin-top: clamp(100px, 18vh, 185px); }
    .visual-copy .eyebrow { color: #ffd2ad; }
    .visual-copy h1 { margin: 15px 0 20px; font-size: clamp(2.8rem, 5vw, 5rem); line-height: 1.05; letter-spacing: -.04em; }
    .visual-copy h1 em { color: #ffd2ad; font-style: normal; }
    .visual-copy p:not(.eyebrow) { max-width: 375px; color: #ffe8d9; line-height: 1.7; font-size: 1.05rem; }
    .food-scene { position: absolute; right: 7%; bottom: -16px; width: min(440px, 70%); height: 345px; }
    .glow { position: absolute; inset: 15px 20px 20px 0; border-radius: 50%; background: #f9d99d2b; filter: blur(2px); }
    .plate { position: absolute; display: grid; place-items: center; border-radius: 50%; background: #fffaf0; box-shadow: 0 25px 45px #4e0d1738, inset 0 0 0 12px #fff, inset 0 0 0 15px #e7a067; }
    .plate span { filter: drop-shadow(0 8px 8px #762b1d44); }
    .plate-large { left: 16px; bottom: 30px; width: 280px; height: 280px; transform: rotate(-8deg); font-size: 7rem; }
    .plate-small { right: 12px; top: 5px; width: 145px; height: 145px; transform: rotate(12deg); font-size: 3.7rem; }
    .food-note { position: absolute; right: 8px; bottom: 18px; display: flex; flex-direction: column; gap: 3px; padding: 15px 18px; border: 1px solid #ffffff50; border-radius: 12px; background: #721b22d9; box-shadow: 0 12px 25px #4e0d1738; }
    .food-note small { color: #ffd2ad; }
    .auth-panel { display: flex; flex-direction: column; justify-content: space-between; min-height: 100vh; padding: 38px clamp(28px, 7vw, 110px); background: #fff; }
    .mobile-brand { display: none; color: var(--ink); }
    .auth-content { width: min(410px, 100%); margin: auto; }
    .auth-content h2 { margin: 12px 0 13px; font-size: clamp(2.25rem, 4vw, 3.4rem); line-height: 1.1; letter-spacing: -.035em; }
    .supporting-text { margin: 0; color: var(--muted); line-height: 1.65; font-size: 1.02rem; }
    .google-button { display: flex; align-items: center; justify-content: center; gap: 12px; width: 100%; min-height: 56px; margin-top: 34px; padding: 0 20px; border: 1px solid #ded5d0; border-radius: 10px; background: #fff; color: var(--ink); font-weight: 700; cursor: pointer; box-shadow: 0 5px 16px #30130c0d; transition: transform .2s, box-shadow .2s, border-color .2s; }
    .google-button:hover:not(:disabled) { transform: translateY(-2px); border-color: #bd2f2f88; box-shadow: 0 12px 25px #8d202326; }
    .google-button:focus-visible, .switch-copy a:focus-visible, .visual-brand:focus-visible, .mobile-brand:focus-visible { outline: 3px solid #f3a681; outline-offset: 4px; }
    .google-button:disabled { cursor: wait; opacity: .7; }
    .google-icon { display: grid; place-items: center; width: 25px; height: 25px; border: 1px solid #e1d8d2; border-radius: 50%; color: #4285f4; font: 700 1rem Arial, sans-serif; }
    .arrow { margin-left: auto; color: var(--red); font-size: 1.2rem; }
    .trust-note { display: flex; justify-content: center; align-items: center; gap: 8px; margin-top: 18px; color: #968984; font-size: .78rem; }
    .lock-icon { color: var(--red); font-size: 1.05rem; transform: rotate(90deg); }
    .switch-copy { margin: 34px 0 0; color: var(--muted); text-align: center; font-size: .9rem; }
    .switch-copy a { margin-left: 4px; color: var(--red); font-weight: 700; }
    .oauth-error { display: flex; align-items: center; gap: 10px; margin-top: 24px; padding: 12px 14px; border: 1px solid #e6aaa1; border-radius: 9px; color: #8d2023; background: #fff4f1; font-size: .86rem; line-height: 1.4; }
    .error-icon { display: grid; place-items: center; flex: 0 0 20px; width: 20px; height: 20px; border-radius: 50%; background: #bd2f2f; color: #fff; font-weight: 800; }
    .legal-copy { margin: 25px 0 0; color: #a29792; text-align: center; font-size: .72rem; line-height: 1.5; }
    .legal-copy a { color: var(--muted); text-decoration: underline; text-underline-offset: 2px; }
    @media (max-width: 800px) {
      .auth-page { display: block; }
      .auth-visual { min-height: 270px; padding: 24px; }
      .visual-copy { margin-top: 42px; }
      .visual-copy h1 { margin: 10px 0; font-size: 2.45rem; }
      .visual-copy p:not(.eyebrow) { display: none; }
      .food-scene { right: -24px; bottom: -130px; width: 255px; height: 220px; opacity: .82; }
      .plate-large { width: 175px; height: 175px; font-size: 4.5rem; }
      .plate-small { width: 90px; height: 90px; font-size: 2.3rem; }
      .food-note { display: none; }
      .auth-panel { min-height: calc(100vh - 270px); padding: 26px 24px 22px; }
      .mobile-brand { display: inline-flex; }
      .auth-content { margin: 54px auto auto; }
    }
    @media (prefers-reduced-motion: reduce) { .google-button { transition: none; } }
  `]
})
export class AuthPage {
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  readonly mode = (this.route.snapshot.data['mode'] ?? 'login') as AuthMode;
  readonly isSigningIn = signal(false);
  readonly oauthError = this.route.snapshot.queryParamMap.has('error')
    ? 'Google sign-in was not completed. Please try again.'
    : '';

  googleSignIn() {
    if (this.isSigningIn()) return;
    this.isSigningIn.set(true);
    this.auth.startGoogleSignIn();
  }
}
