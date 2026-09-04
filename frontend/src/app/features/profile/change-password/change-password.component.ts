import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { UserService } from '../../../core/services/user.service';

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './change-password.component.html',
  styleUrl: './change-password.component.css',
})
export class ChangePasswordComponent {
  currentPassword = '';
  newPassword = '';
  confirmNewPassword = '';

  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  constructor(private userService: UserService, private router: Router) {}

  submit() {
    this.errorMessage.set(null);
    this.successMessage.set(null);

    if (!this.currentPassword || !this.newPassword) {
      this.errorMessage.set('Vui lòng nhập đầy đủ thông tin.');
      return;
    }
    if (this.newPassword !== this.confirmNewPassword) {
      this.errorMessage.set('Mật khẩu mới nhập lại không khớp.');
      return;
    }

    this.loading.set(true);
    this.userService
      .changePassword({
        currentPassword: this.currentPassword,
        newPassword: this.newPassword,
        confirmNewPassword: this.confirmNewPassword,
      })
      .subscribe({
        next: () => {
          this.loading.set(false);
          this.successMessage.set('Đổi mật khẩu thành công.');
          this.currentPassword = '';
          this.newPassword = '';
          this.confirmNewPassword = '';
        },
        error: (err) => {
          this.loading.set(false);
          this.errorMessage.set(err?.error?.message ?? 'Đổi mật khẩu thất bại.');
        },
      });
  }
}
