import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { User } from '../models/user.model';

export interface RegisterPayload {
  email: string;
  googleIdToken: string;
  password: string;
  confirmPassword: string;
  fullName: string;
  phoneNumber?: string;
}

export interface LoginPayload {
  email: string;
  password: string;
}

/**
 * Holds auth state on the client (the actual session lives in the httpOnly
 * cookie set by the backend — this signal just mirrors "am I logged in"
 * for route guards and the UI).
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly baseUrl = `${environment.apiBaseUrl}/auth`;

  readonly currentUser = signal<User | null>(null);

  constructor(private http: HttpClient) {}

  /**
   * Step 3: the backend now sets the JWT cookie on successful registration
   * too (same as login), so the wizard can immediately continue into an
   * authenticated real-passkey-setup step without a separate login call.
   */
  register(payload: RegisterPayload): Observable<User> {
    return this.http
      .post<User>(`${this.baseUrl}/register`, payload, { withCredentials: true })
      .pipe(tap((user) => this.currentUser.set(user)));
  }

  login(payload: LoginPayload): Observable<User> {
    return this.http
      .post<User>(`${this.baseUrl}/login`, payload, { withCredentials: true })
      .pipe(tap((user) => this.currentUser.set(user)));
  }

  logout(): Observable<void> {
    return this.http
      .post<void>(`${this.baseUrl}/logout`, {}, { withCredentials: true })
      .pipe(tap(() => this.currentUser.set(null)));
  }

  /** Step 2: real Google login. `idToken` comes from GoogleAuthService's callback. */
  loginWithGoogle(idToken: string): Observable<User> {
    return this.http
      .post<User>(`${this.baseUrl}/oauth2/google`, { idToken }, { withCredentials: true })
      .pipe(tap((user) => this.currentUser.set(user)));
  }

  /**
   * Verifies a Google ID token during the registration wizard's email step
   * and returns the email it belongs to. Does not log in / create anything.
   */
  verifyGoogleEmail(idToken: string): Observable<{ email: string }> {
    return this.http.post<{ email: string }>(`${this.baseUrl}/oauth2/verify-email`, { idToken });
  }
}
