import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { PasskeyService } from '../../../core/services/passkey.service';
import { UserService } from '../../../core/services/user.service';

/**
 * Behavior per spec: if the user has no passkey -> offer "Thêm" (goes to the
 * real WebAuthn setup ceremony) or "Không" (back to menu, console log). If
 * they have one -> offer "Xóa" (calls backend, then back to menu either way,
 * console log) or "Không".
 */
@Component({
  selector: 'app-passkey-manage',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './passkey-manage.component.html',
  styleUrl: './passkey-manage.component.css',
})
export class PasskeyManageComponent implements OnInit {
  readonly hasPasskey = signal(false);
  readonly working = signal(false);
  readonly errorMessage = signal<string | null>(null);

  constructor(
    private router: Router,
    private authService: AuthService,
    private userService: UserService,
    private passkeyService: PasskeyService
  ) {}

  ngOnInit() {
    const current = this.authService.currentUser();
    if (current) {
      this.hasPasskey.set(current.passkeyEnabled);
      return;
    }
    // Fallback in case this page is opened before /users/me has ever been
    // fetched in this session (authGuard already guarantees a valid cookie).
    this.userService.getMe().subscribe((user) => {
      this.authService.currentUser.set(user);
      this.hasPasskey.set(user.passkeyEnabled);
    });
  }

  decline() {
    console.log('[passkey] Người dùng chọn không thay đổi Passkey.');
    this.router.navigate(['/menu']);
  }

  addPasskey() {
    this.router.navigate(['/profile/passkey-setup']);
  }

  removePasskey() {
    this.working.set(true);
    this.errorMessage.set(null);

    this.passkeyService.deletePasskey().subscribe({
      next: () => {
        this.working.set(false);
        console.log('[passkey] Xóa Passkey thành công.');
        const current = this.authService.currentUser();
        if (current) {
          this.authService.currentUser.set({ ...current, passkeyEnabled: false });
        }
        this.router.navigate(['/menu']);
      },
      error: (err) => {
        this.working.set(false);
        console.log('[passkey] Xóa Passkey thất bại:', err?.error?.message ?? err?.message ?? err);
        this.router.navigate(['/menu']);
      },
    });
  }
}
