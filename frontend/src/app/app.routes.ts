import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  // Rotas Públicas Institucionais
  {
    path: '',
    loadComponent: () => import('./features/public/home/home.component').then(m => m.HomeComponent),
    title: 'WB Agency | Model Management & Editorial Casting'
  },
  {
    path: 'models',
    loadComponent: () => import('./features/public/models/models.component').then(m => m.ModelsComponent),
    title: 'Casting de Modelos | WB Scouting'
  },
  {
    path: 'models/female',
    loadComponent: () => import('./features/public/models/models.component').then(m => m.ModelsComponent),
    title: 'Casting Feminino | WB Scouting'
  },
  {
    path: 'models/male',
    loadComponent: () => import('./features/public/models/models.component').then(m => m.ModelsComponent),
    title: 'Casting Masculino | WB Scouting'
  },
  {
    path: 'models/stars',
    loadComponent: () => import('./features/public/models/models.component').then(m => m.ModelsComponent),
    title: 'Stars | WB Scouting'
  },
  {
    path: 'models/:slug',
    loadComponent: () => import('./features/public/model-detail/model-detail.component').then(m => m.ModelDetailComponent),
    title: 'Perfil do Modelo | WB Scouting'
  },
  {
    path: 'apply',
    loadComponent: () => import('./features/public/apply/apply.component').then(m => m.ApplyComponent),
    title: 'Quero Ser Modelo | Inscrição de Novos Talentos'
  },
  {
    path: 'contact',
    loadComponent: () => import('./features/public/contact/contact.component').then(m => m.ContactComponent),
    title: 'Contato & Casting Comercial | WB Agency'
  },

  // Rotas de Autenticação Administrativa
  {
    path: 'admin/login',
    loadComponent: () => import('./features/admin/auth/login/login.component').then(m => m.LoginComponent),
    title: 'Acesso Administrativo | WB Agency'
  },
  {
    path: 'admin/forgot-password',
    loadComponent: () => import('./features/admin/auth/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent),
    title: 'Recuperação de Senha | WB Agency'
  },
  {
    path: 'admin/reset-password',
    loadComponent: () => import('./features/admin/auth/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent),
    title: 'Redefinição de Senha | WB Agency'
  },

  // Rotas Protegidas do Backoffice (CMS)
  {
    path: 'admin',
    redirectTo: 'admin/dashboard',
    pathMatch: 'full'
  },
  {
    path: 'admin/dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./features/admin/dashboard/dashboard.component').then(m => m.DashboardComponent),
    title: 'Painel de Controle | WB Agency'
  },
  {
    path: 'admin/models',
    canActivate: [authGuard],
    loadComponent: () => import('./features/admin/models-mgmt/models-mgmt.component').then(m => m.ModelsMgmtComponent),
    title: 'Gestão de Modelos | WB Agency'
  },
  {
    path: 'admin/candidates',
    canActivate: [authGuard],
    loadComponent: () => import('./features/admin/candidates-mgmt/candidates-mgmt.component').then(m => m.CandidatesMgmtComponent),
    title: 'Triagem de Candidaturas | WB Agency'
  },
  {
    path: 'admin/content',
    canActivate: [authGuard],
    loadComponent: () => import('./features/admin/content-mgmt/content-mgmt.component').then(m => m.ContentMgmtComponent),
    title: 'Gestão de Conteúdo & Vídeo | WB Agency'
  },

  // Fallback para rota inicial
  {
    path: '**',
    redirectTo: ''
  }
];
