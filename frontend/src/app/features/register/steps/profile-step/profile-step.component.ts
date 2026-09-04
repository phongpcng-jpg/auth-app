import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { RegisterStateService } from '../../state/register-state.service';

@Component({
  selector: 'app-register-profile-step',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile-step.component.html',
  styleUrl: './profile-step.component.css',
})
export class ProfileStepComponent implements OnInit {
  fullName = '';
  phoneNumber = '';
  readonly errorMessage = signal<string | null>(null);

  constructor(private state: RegisterStateService, private router: Router) {}

  ngOnInit() {
    if (!this.state.hasPassword()) {
      this.router.navigate(['/register/password']);
    }
  }

  submit() {
    this.errorMessage.set(null);

    if (!this.fullName.trim()) {
      this.errorMessage.set('Họ và tên là bắt buộc.');
      return;
    }

    this.state.setProfile(this.fullName.trim(), this.phoneNumber.trim());
    this.router.navigate(['/register/passkey-prompt']);
  }
}
