import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { RegisterStateService } from '../../state/register-state.service';

@Component({
  selector: 'app-register-password-step',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './password-step.component.html',
  styleUrl: './password-step.component.css',
})
export class PasswordStepComponent implements OnInit {
  password = '';
  confirmPassword = '';
  readonly errorMessage = signal<string | null>(null);

  constructor(private state: RegisterStateService, private router: Router) {}

  ngOnInit() {
    // A user can't reach this step directly without a verified email.
    if (!this.state.hasVerifiedEmail()) {
      this.router.navigate(['/register/email']);
    }
  }

  submit() {
    this.errorMessage.set(null);

    if (!this.password || this.password.length < 6) {
      this.errorMessage.set('Mật khẩu phải có ít nhất 6 ký tự.');
      return;
    }
    if (this.password !== this.confirmPassword) {
      this.errorMessage.set('Mật khẩu nhập lại không khớp.');
      return;
    }

    this.state.setPassword(this.password, this.confirmPassword);
    this.router.navigate(['/register/profile']);
  }
}
