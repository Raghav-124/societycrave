import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { CallbackPage } from './callback.page';

describe('CallbackPage', () => {
  let fixture: ComponentFixture<CallbackPage>;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [CallbackPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: new URLSearchParams() } } }
      ]
    });
    fixture = TestBed.createComponent(CallbackPage);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('verifies the backend session after OAuth redirect', () => {
    const request = http.expectOne('/api/auth/me');
    request.flush({ id: '1', email: 'person@example.com', displayName: 'Person' });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('You are signed in.');
  });
});
