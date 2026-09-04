import { CommonModule } from '@angular/common';
import { AfterViewInit, Component, ElementRef, signal, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { GoogleAuthService } from '../../../../core/services/google-auth.service';
import { RegisterStateService } from '../../state/register-state.service';

/**
 * Step 1 of 4: enter (expected) email, verify it via real Google OAuth2.
 * The email typed here is only used as a "login_hint" to bias Google's
 * account chooser — the email that actually gets locked in and carried
 * forward is whatever Google's ID token reports, per product decision.
 */
@Component({
  selector: 'app-register-email-step',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './email-step.component.html',
  styleUrl: './email-step.component.css',
})
export class EmailStepComponent implements AfterViewInit {
  @ViewChild('googleBtnContainer') googleBtnContainer?: ElementRef<HTMLDivElement>;

  email = '';
  readonly errorMessage = signal<string | null>(null);
  readonly verifying = signal(false);
  readonly verified = signal(false);

  constructor(
    private state: RegisterStateService,
    private authService: AuthService,
    private googleAuthService: GoogleAuthService,
    private router: Router
  ) {
    const existing = this.state.data();
    this.email = existing.email;
    this.verified.set(existing.emailVerified);
  }

  ngAfterViewInit() {
    this.renderGoogleButton();
  }

  /** Re-renders Google's button with the currently typed email as a hint. */
  renderGoogleButton() {
    if (!this.googleBtnContainer) return;
    this.googleAuthService.renderOverlayButton(
      this.googleBtnContainer.nativeElement,
      (idToken) => this.onGoogleCredential(idToken),
      this.email.trim() || undefined
    );
  }

  private onGoogleCredential(idToken: string) {
    this.errorMessage.set(null);
    this.verifying.set(true);

    this.authService.verifyGoogleEmail(idToken).subscribe({
      next: ({ email }) => {
        this.verifying.set(false);
        this.verified.set(true);
        this.email = email; // lock to whatever Google actually confirmed
        this.state.setEmail(email, true, idToken);
      },
      error: (err) => {
        this.verifying.set(false);
        this.verified.set(false);
        this.errorMessage.set(err?.error?.message ?? 'Không thể xác thực email qua Google. Vui lòng thử lại.');
      },
    });
  }

  next() {
    if (!this.verified()) {
      this.errorMessage.set('Vui lòng xác thực email qua Google trước khi tiếp tục.');
      return;
    }
    this.router.navigate(['/register/password']);
  }
}
