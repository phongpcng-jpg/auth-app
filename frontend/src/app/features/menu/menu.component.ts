import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

interface MenuItem {
  label: string;
  route: string;
  enabled: boolean;
}

/**
 * Menu is rendered from this list, so adding a new feature later is just
 * adding one entry here (set enabled: true) — no layout changes needed.
 */
const MENU_ITEMS: MenuItem[] = [
  { label: 'Thông tin cá nhân', route: '/profile', enabled: true },
  { label: 'Đổi mật khẩu', route: '/profile/change-password', enabled: true },
  { label: 'Quản lý Passkey', route: '/profile/passkey', enabled: true },
];

@Component({
  selector: 'app-menu',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './menu.component.html',
  styleUrl: './menu.component.css',
})
export class MenuComponent {
  readonly items = MENU_ITEMS.filter((item) => item.enabled);

  constructor(private router: Router, private authService: AuthService) {}

  go(route: string) {
    this.router.navigate([route]);
  }

  logout() {
    this.authService.logout().subscribe({
      next: () => this.router.navigate(['/login']),
      error: () => this.router.navigate(['/login']),
    });
  }
}
