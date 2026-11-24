import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AuthService, provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads the verified current session', () => {
    service.loadCurrentUser().subscribe();
    const request = http.expectOne('/api/auth/me');
    expect(request.request.withCredentials).toBe(true);
    request.flush({ id: '1', email: 'person@example.com', displayName: 'Person', onboardingStatus: 'ONBOARDING_REQUIRED' });
    expect(service.currentUser()?.email).toBe('person@example.com');
  });

  it('marks unauthenticated state when the session is not found', () => {
    service.loadCurrentUser().subscribe({ error: () => undefined });
    const request = http.expectOne('/api/auth/me');
    request.flush(null, { status: 401, statusText: 'Unauthorized' });
    expect(service.currentUser()).toBeNull();
    expect(service.loading()).toBe(false);
    expect(service.initialized()).toBe(true);
  });

  it('clears the current session after logout', () => {
    service.currentUser.set({ id: '1', email: 'person@example.com', displayName: 'Person', onboardingStatus: 'ACTIVE_MEMBERSHIP' });
    service.logout().subscribe();
    const request = http.expectOne('/api/auth/logout');
    expect(request.request.method).toBe('POST');
    expect(request.request.withCredentials).toBe(true);
    request.flush(null);
    expect(service.currentUser()).toBeNull();
  });
});
