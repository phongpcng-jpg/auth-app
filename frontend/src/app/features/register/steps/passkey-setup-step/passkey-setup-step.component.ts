import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { PasskeyService } from '../../../../core/services/passkey.service';

/**
 * Real WebAuthn registration ceremony, run right after account creation
 * (the account already exists and the user is already logged in — see
 * passkey-prompt-step.component.ts).
 */
@Component({
  selector: 'app-register-passkey-setup-step',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './passkey-setup-step.component.html',
  styleUrl: './passkey-setup-step.component.css',
})
export class PasskeySetupStepComponent implements OnInit {
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  constructor(
    private authService: AuthService,
    private passkeyService: PasskeyService,
    private router: Router
  ) {}

  ngOnInit() {
    if (!this.authService.currentUser()) {
      this.router.navigate(['/login']);
    }
  }

  setupPasskey() {
    this.errorMessage.set(null);
    this.submitting.set(true);

    this.passkeyService.setupPasskey().subscribe({
      next: () => {
        this.submitting.set(false);
        console.log('[passkey] Thiết lập passkey thành công.');
        this.router.navigate(['/menu']);
      },
      error: (err) => {
        this.submitting.set(false);
        if (err?.name === 'NotAllowedError') {
          console.log('[passkey] Người dùng đã huỷ thiết lập passkey.');
          this.errorMessage.set('Bạn đã huỷ thao tác. Có thể thử lại hoặc bỏ qua để vào menu.');
          return;
        }
        console.log('[passkey] Thiết lập passkey thất bại.');
        this.errorMessage.set(
          err?.error?.message ?? err?.message ?? 'Thiết lập Passkey thất bại. Bạn có thể thử lại hoặc bỏ qua.'
        );
      },
    });
  }

  skip() {
    this.router.navigate(['/menu']);
  }
}
