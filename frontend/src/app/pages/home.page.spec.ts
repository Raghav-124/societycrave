import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { HomePage } from './home.page';

describe('HomePage authentication header', () => {
  let fixture: ComponentFixture<HomePage>;
  let http: HttpTestingController;
  let auth: AuthService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HomePage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(HomePage);
    http = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => http.verify());

  it('shows loading, then replaces Log in with the authenticated account', () => {
    auth.loadCurrentUser().subscribe();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).not.toContain('Log in');

    http.expectOne('/api/auth/me').flush({
      id: '1',
      email: 'person@example.com',
      displayName: 'Person',
      onboardingStatus: 'ONBOARDING_REQUIRED'
    });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Person');
    expect(fixture.nativeElement.textContent).not.toContain('Log in');
  });

  it('shows Log in after an unauthenticated session response', () => {
    auth.loadCurrentUser().subscribe({ error: () => undefined });
    http.expectOne('/api/auth/me').flush(null, { status: 401, statusText: 'Unauthorized' });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Log in');
  });
});
