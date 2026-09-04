import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { User } from '../../../core/models/user.model';
import { UserService } from '../../../core/services/user.service';

@Component({
  selector: 'app-profile-view',
  standalone: true,
  imports: [CommonModule, DatePipe, RouterLink],
  templateUrl: './profile-view.component.html',
  styleUrl: './profile-view.component.css',
})
export class ProfileViewComponent implements OnInit {
  readonly user = signal<User | null>(null);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  constructor(private userService: UserService, private router: Router) {}

  ngOnInit() {
    this.userService.getMe().subscribe({
      next: (user) => {
        this.user.set(user);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.errorMessage.set('Không tải được thông tin cá nhân.');
      },
    });
  }
}
