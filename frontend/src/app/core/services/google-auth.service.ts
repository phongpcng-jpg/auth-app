import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';

// Google Identity Services is loaded globally via the <script> tag in
// index.html (not an npm package), so we declare its shape loosely here
// instead of pulling in @types for a handful of calls.
declare const google: {
  accounts: {
    id: {
      initialize(config: {
        client_id: string;
        callback: (response: { credential: string }) => void;
        login_hint?: string;
      }): void;
      renderButton(
        parent: HTMLElement,
        options: { type?: string; theme?: string; size?: string; width?: number; text?: string }
      ): void;
    };
  };
};

/**
 * Thin wrapper around Google Identity Services (GSI).
 *
 * The app's buttons are minimally styled to match `theme.css`, but Google's
 * terms require the actual sign-in click to go through their widget. To
 * reconcile the two, `renderOverlayButton` renders Google's real button
 * transparently on top of our own styled button (see the .google-btn-overlay
 * class next to each usage) — the user sees our design, the click lands on
 * Google's iframe underneath.
 */
@Injectable({ providedIn: 'root' })
export class GoogleAuthService {
  isReady(): boolean {
    return typeof google !== 'undefined' && !!google?.accounts?.id;
  }

  /**
   * Renders an invisible, full-size Google Sign-In button inside `container`
   * and wires its result to `onCredential` (called with the raw Google ID
   * token JWT). `loginHint` optionally pre-selects/biases the Google account
   * chooser toward an email the user already typed.
   */
  renderOverlayButton(
    container: HTMLElement,
    onCredential: (idToken: string) => void,
    loginHint?: string
  ): void {
    if (!this.isReady()) {
      console.error('[google-auth] Google Identity Services chưa sẵn sàng (script chưa tải xong hoặc bị chặn).');
      return;
    }

    google.accounts.id.initialize({
      client_id: environment.googleClientId,
      callback: (response) => onCredential(response.credential),
      ...(loginHint ? { login_hint: loginHint } : {}),
    });

    container.innerHTML = '';
    google.accounts.id.renderButton(container, {
      type: 'standard',
      theme: 'outline',
      size: 'large',
      width: 320,
    });
  }
}
