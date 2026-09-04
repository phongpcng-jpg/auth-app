import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { LoginComponent } from './features/login/login.component';
import { MenuComponent } from './features/menu/menu.component';
import { ChangePasswordComponent } from './features/profile/change-password/change-password.component';
import { PasskeyManageComponent } from './features/profile/passkey-manage/passkey-manage.component';
import { ProfileViewComponent } from './features/profile/profile-view/profile-view.component';
import { EmailStepComponent } from './features/register/steps/email-step/email-step.component';
import { PasskeyPromptStepComponent } from './features/register/steps/passkey-prompt-step/passkey-prompt-step.component';
import { PasskeySetupStepComponent } from './features/register/steps/passkey-setup-step/passkey-setup-step.component';
import { PasswordStepComponent } from './features/register/steps/password-step/password-step.component';
import { ProfileStepComponent } from './features/register/steps/profile-step/profile-step.component';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: 'login', component: LoginComponent },

  { path: 'register/email', component: EmailStepComponent },
  { path: 'register/password', component: PasswordStepComponent },
  { path: 'register/profile', component: ProfileStepComponent },
  { path: 'register/passkey-prompt', component: PasskeyPromptStepComponent },
  { path: 'register/passkey-setup', component: PasskeySetupStepComponent },

  { path: 'menu', component: MenuComponent, canActivate: [authGuard] },
  { path: 'profile', component: ProfileViewComponent, canActivate: [authGuard] },
  { path: 'profile/change-password', component: ChangePasswordComponent, canActivate: [authGuard] },
  { path: 'profile/passkey', component: PasskeyManageComponent, canActivate: [authGuard] },
  { path: 'profile/passkey-setup', component: PasskeySetupStepComponent, canActivate: [authGuard] },

  { path: '**', redirectTo: 'login' },
];
