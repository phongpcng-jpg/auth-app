import { CommonModule } from '@angular/common';
import { AfterViewInit, Component, ElementRef, signal, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { GoogleAuthService } from '../../core/services/google-auth.service';
import { PasskeyService } from '../../core/services/passkey.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
})
export class LoginComponent implements AfterViewInit {
  @ViewChild('googleBtnContainer') googleBtnContainer?: ElementRef<HTMLDivElement>;

  email = '';
  password = '';

  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly passkeySupported: boolean;

  constructor(
    private authService: AuthService,
    private googleAuthService: GoogleAuthService,
    private passkeyService: PasskeyService,
    private router: Router
  ) {
    this.passkeySupported = this.passkeyService.isSupported();
  }

  ngAfterViewInit() {
    if (this.googleBtnContainer) {
      this.googleAuthService.renderOverlayButton(this.googleBtnContainer.nativeElement, (idToken) =>
        this.onGoogleCredential(idToken)
      );
    }
  }

  submit() {
    this.errorMessage.set(null);

    if (!this.email || !this.password) {
      this.errorMessage.set('Vui lòng nhập đầy đủ email và mật khẩu.');
      return;
    }

    this.loading.set(true);
    this.authService.login({ email: this.email, password: this.password }).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/menu']);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err?.error?.message ?? 'Đăng nhập thất bại. Vui lòng thử lại.');
      },
    });
  }

  private onGoogleCredential(idToken: string) {
    this.errorMessage.set(null);
    this.loading.set(true);

    this.authService.loginWithGoogle(idToken).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/menu']);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err?.error?.message ?? 'Đăng nhập bằng Google thất bại. Vui lòng thử lại.');
      },
    });
  }

  loginWithPasskey() {
    this.errorMessage.set(null);
    this.loading.set(true);

    this.passkeyService.loginWithPasskey().subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/menu']);
      },
      error: (err) => {
        this.loading.set(false);
        // NotAllowedError (user cancelled the browser's passkey picker) is not
        // an error worth showing as a form error — just log it, like the
        // other quiet-cancel paths in this app.
        if (err?.name === 'NotAllowedError') {
          console.log('[passkey] Người dùng đã huỷ đăng nhập bằng passkey.');
          return;
        }
        this.errorMessage.set(err?.error?.message ?? err?.message ?? 'Đăng nhập bằng Passkey thất bại.');
      },
    });
  }
}
