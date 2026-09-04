import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, from, switchMap, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { User } from '../models/user.model';
import {
  decodeCreationOptions,
  decodeRequestOptions,
  encodeAssertionCredential,
  encodeRegistrationCredential,
} from '../utils/webauthn-codec';
import { AuthService } from './auth.service';

interface PasskeyOptionsResponse {
  requestId: string;
  publicKey: any;
}

/**
 * Runs the two WebAuthn ceremonies (registration = "add a passkey",
 * authentication = "log in with a passkey") end to end: fetch options from
 * the backend, call the browser's native `navigator.credentials` API, send
 * the result back for verification.
 */
@Injectable({ providedIn: 'root' })
export class PasskeyService {
  private readonly baseUrl = environment.apiBaseUrl;

  constructor(private http: HttpClient, private authService: AuthService) {}

  /** WebAuthn isn't available on the API server, very old browsers, or non-secure (non-HTTPS/non-localhost) origins. */
  isSupported(): boolean {
    return (
      typeof window !== 'undefined' &&
      !!window.PublicKeyCredential &&
      // Cast: see webauthn-codec.ts — TS's bundled DOM typings don't yet
      // declare these WebAuthn Level 3 methods, though browsers support them.
      typeof (PublicKeyCredential as any).parseCreationOptionsFromJSON === 'function' &&
      typeof (PublicKeyCredential as any).parseRequestOptionsFromJSON === 'function'
    );
  }

  /** Requires the caller to already be authenticated (JWT cookie set) — see PasskeyController on the backend. */
  setupPasskey(deviceName?: string): Observable<void> {
    return this.http
      .post<PasskeyOptionsResponse>(`${this.baseUrl}/passkey/register/options`, {}, { withCredentials: true })
      .pipe(
        switchMap((options) =>
          from(this.createCredential(options)).pipe(
            switchMap((credentialJson) =>
              this.http.post<void>(
                `${this.baseUrl}/passkey/register/verify`,
                { requestId: options.requestId, credential: credentialJson, deviceName },
                { withCredentials: true }
              )
            )
          )
        )
      );
  }

  deletePasskey(): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/passkey`, { withCredentials: true });
  }

  /** Usernameless: the browser's own passkey picker identifies the account, so no email is needed here. */
  loginWithPasskey(): Observable<User> {
    return this.http
      .post<PasskeyOptionsResponse>(`${this.baseUrl}/auth/passkey/login/options`, {}, { withCredentials: true })
      .pipe(
        switchMap((options) =>
          from(this.getAssertion(options)).pipe(
            switchMap((credentialJson) =>
              this.http.post<User>(
                `${this.baseUrl}/auth/passkey/login/verify`,
                { requestId: options.requestId, credential: credentialJson },
                { withCredentials: true }
              )
            )
          )
        ),
        tap((user) => this.authService.currentUser.set(user))
      );
  }

  private async createCredential(options: PasskeyOptionsResponse): Promise<unknown> {
    const publicKeyJson = this.normalize(options.publicKey);
    console.log('[passkey] register options from server:', publicKeyJson);
    const publicKey = decodeCreationOptions(publicKeyJson);
    const credential = (await navigator.credentials.create({ publicKey })) as PublicKeyCredential | null;
    if (!credential) {
      throw new Error('Không thể tạo passkey (bị huỷ hoặc trình duyệt từ chối).');
    }
    return encodeRegistrationCredential(credential);
  }

  private async getAssertion(options: PasskeyOptionsResponse): Promise<unknown> {
    const publicKeyJson = this.normalize(options.publicKey);
    console.log('[passkey] login options from server:', publicKeyJson);
    const publicKey = decodeRequestOptions(publicKeyJson);
    const credential = (await navigator.credentials.get({ publicKey })) as PublicKeyCredential | null;
    if (!credential) {
      throw new Error('Xác thực bằng passkey đã bị huỷ.');
    }
    return encodeAssertionCredential(credential);
  }

  /**
   * Defensive: if `publicKey` somehow arrived as a JSON *string* instead of
   * an already-parsed object (e.g. double-encoded on the way through the
   * backend DTO), parse it here rather than handing a string straight to
   * the browser's WebAuthn parser (which would silently look like every
   * required field is "undefined").
   */
  private normalize(value: any): any {
    return typeof value === 'string' ? JSON.parse(value) : value;
  }
}
