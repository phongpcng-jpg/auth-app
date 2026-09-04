import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { RegisterStateService } from '../../state/register-state.service';

/**
 * Last step of the wizard. Either way the account is created right here
 * (POST /api/auth/register, which now also logs the user in) — "Có" then
 * continues into the real WebAuthn setup step, "Không" goes straight to the
 * menu, matching the behavior described for Step 3.
 */
@Component({
  selector: 'app-register-passkey-prompt-step',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './passkey-prompt-step.component.html',
  styleUrl: './passkey-prompt-step.component.css',
})
export class PasskeyPromptStepComponent implements OnInit {
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  constructor(
    private state: RegisterStateService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit() {
    if (!this.state.hasProfile()) {
      this.router.navigate(['/register/profile']);
    }
  }

  chooseYes() {
    this.createAccount(() => this.router.navigate(['/register/passkey-setup']));
  }

  chooseNo() {
    this.createAccount(() => this.router.navigate(['/menu']));
  }

  private createAccount(onSuccess: () => void) {
    this.errorMessage.set(null);
    this.submitting.set(true);

    this.authService.register(this.state.toRegisterPayload()).subscribe({
      next: () => {
        this.submitting.set(false);
        this.state.reset();
        onSuccess();
      },
      error: (err) => {
        this.submitting.set(false);
        this.errorMessage.set(err?.error?.message ?? 'Đăng ký thất bại. Vui lòng thử lại.');
      },
    });
  }
}
