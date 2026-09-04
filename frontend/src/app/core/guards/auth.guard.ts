import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { UserService } from '../services/user.service';

/**
 * Confirms the httpOnly session cookie is still valid by calling /users/me.
 * Redirects to /login if not authenticated.
 */
export const authGuard: CanActivateFn = () => {
  const userService = inject(UserService);
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.currentUser()) {
    return true;
  }

  return userService.getMe().pipe(
    map((user) => {
      authService.currentUser.set(user);
      return true;
    }),
    catchError(() => {
      router.navigate(['/login']);
      return of(false);
    })
  );
};
