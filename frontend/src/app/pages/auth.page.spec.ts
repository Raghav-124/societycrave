import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { AuthPage } from './auth.page';

describe('AuthPage', () => {
  let fixture: ComponentFixture<AuthPage>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AuthPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              data: { mode: 'signup' },
              queryParamMap: new URLSearchParams()
            }
          }
        }
      ]
    });
    fixture = TestBed.createComponent(AuthPage);
    fixture.detectChanges();
  });

  it('renders the signup experience instead of the login copy', () => {
    expect(fixture.nativeElement.textContent).toContain('Create your account.');
    expect(fixture.nativeElement.textContent).toContain('Join your society’s homemade food community.');
    expect(fixture.nativeElement.textContent).toContain('After Google authentication, choose your society and set up your membership.');
    expect(fixture.nativeElement.textContent).toContain('Already have an account?');
  });

  it('does not render password authentication controls', () => {
    expect(fixture.nativeElement.querySelector('input[type="password"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('button').textContent).toContain('Continue with Google');
  });
});
