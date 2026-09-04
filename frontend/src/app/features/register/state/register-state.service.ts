import { Injectable, signal } from '@angular/core';

export interface RegisterWizardData {
  email: string;
  emailVerified: boolean;
  /** Raw Google ID token from the email step, re-sent (and re-verified server-side) at final submit. */
  googleIdToken: string;
  password: string;
  confirmPassword: string;
  fullName: string;
  phoneNumber: string;
  wantsPasskey: boolean;
}

const EMPTY_STATE: RegisterWizardData = {
  email: '',
  emailVerified: false,
  googleIdToken: '',
  password: '',
  confirmPassword: '',
  fullName: '',
  phoneNumber: '',
  wantsPasskey: false,
};

/**
 * Holds the registration wizard's in-progress data in memory as the user
 * moves between /register/* steps. Nothing is sent to the backend until the
 * final step, which posts everything in one call to POST /api/auth/register.
 *
 * Guards each step (e.g. can't reach the password step without an email)
 * so a user can't skip ahead by typing a URL directly.
 */
@Injectable({ providedIn: 'root' })
export class RegisterStateService {
  private readonly state = signal<RegisterWizardData>({ ...EMPTY_STATE });

  readonly data = this.state.asReadonly();

  setEmail(email: string, verified: boolean, googleIdToken: string) {
    this.state.update((s) => ({ ...s, email, emailVerified: verified, googleIdToken }));
  }

  setPassword(password: string, confirmPassword: string) {
    this.state.update((s) => ({ ...s, password, confirmPassword }));
  }

  setProfile(fullName: string, phoneNumber: string) {
    this.state.update((s) => ({ ...s, fullName, phoneNumber }));
  }

  setWantsPasskey(wantsPasskey: boolean) {
    this.state.update((s) => ({ ...s, wantsPasskey }));
  }

  hasVerifiedEmail(): boolean {
    return !!this.state().email && this.state().emailVerified;
  }

  hasPassword(): boolean {
    return !!this.state().password;
  }

  hasProfile(): boolean {
    return !!this.state().fullName;
  }

  reset() {
    this.state.set({ ...EMPTY_STATE });
  }

  toRegisterPayload() {
    const s = this.state();
    return {
      email: s.email,
      googleIdToken: s.googleIdToken,
      password: s.password,
      confirmPassword: s.confirmPassword,
      fullName: s.fullName,
      phoneNumber: s.phoneNumber || undefined,
      // wantsPasskey is NOT sent — the account is created first, then (if the
      // user opted in) a real WebAuthn ceremony runs as a separate
      // authenticated request. See passkey-setup-step.component.ts.
    };
  }
}
