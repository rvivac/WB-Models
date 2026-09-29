import { Routes } from '@angular/router';
import { adminAuthGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  // Rotas Públicas Institucionais
  {
    path: '',
    loadComponent: () => import('./features/public/home/home.component').then(m => m.HomeComponent),
    title: 'WB Agency | Model Management & Editorial Casting'
  },
  {
    path: 'models',
    redirectTo: 'models/female',
    pathMatch: 'full'
  },
  {
    path: 'modelos',
    redirectTo: 'models/female',
    pathMatch: 'full'
  },
  {
    path: 'models/female',
    loadComponent: () => import('./features/public/models/model-list.component').then(m => m.ModelListComponent),
    data: { gender: 'FEMALE' },
    title: 'Casting Feminino | WB Scouting'
  },
  {
    path: 'models/male',
    loadComponent: () => import('./features/public/models/model-list.component').then(m => m.ModelListComponent),
    data: { gender: 'MALE' },
    title: 'Casting Masculino | WB Scouting'
  },
  {
    path: 'models/stars',
    loadComponent: () => import('./features/public/models/model-list.component').then(m => m.ModelListComponent),
    data: { isStar: true },
    title: 'Stars | WB Scouting'
  },
  {
    path: 'models/:id',
    loadComponent: () => import('./features/public/models/pages/model-detail/model-detail.component').then(m => m.ModelDetailComponent),
    title: 'Perfil do Modelo | WB Scouting'
  },
  {
    path: 'modelos/:id',
    loadComponent: () => import('./features/public/models/pages/model-detail/model-detail.component').then(m => m.ModelDetailComponent),
    title: 'Perfil do Modelo | WB Scouting'
  },
  {
    path: 'apply',
    loadComponent: () => import('./features/candidate-submission/candidate-submission-form.component').then(m => m.CandidateSubmissionFormComponent),
    title: 'Quero Ser Modelo | Inscrição de Novos Talentos'
  },
  {
    path: 'seja-modelo',
    redirectTo: 'apply',
    pathMatch: 'full'
  },
  {
    path: 'candidate-submission',
    redirectTo: 'apply',
    pathMatch: 'full'
  },
  {
    path: 'contact',
    loadComponent: () => import('./features/public/contact/contact.component').then(m => m.ContactComponent),
    title: 'Contato & Casting Comercial | WB Agency'
  },
  {
    path: 'contato',
    loadComponent: () => import('./features/public/contact/contact.component').then(m => m.ContactComponent),
    title: 'Contato & Casting Comercial | WB Agency'
  },

  // Rota de Login Administrativo (Pública)
  {
    path: 'login',
    redirectTo: 'admin/login',
    pathMatch: 'full'
  },
  {
    path: 'admin/login',
    loadComponent: () => import('./features/admin/login/admin-login.component').then(m => m.AdminLoginComponent),
    title: 'Acesso Administrativo | WB Agency'
  },
  {
    path: 'admin/forgot-password',
    loadComponent: () => import('./features/admin/auth/forgot-password.component').then(m => m.ForgotPasswordComponent),
    title: 'Recuperação de Acesso | WB Agency'
  },
  {
    path: 'admin/reset-password',
    loadComponent: () => import('./features/admin/auth/reset-password.component').then(m => m.ResetPasswordComponent),
    title: 'Redefinição de Senha | WB Agency'
  },

  // Rotas Administrativas Protegidas (Backoffice / CMS)
  {
    path: 'admin',
    canActivate: [adminAuthGuard],
    children: [
      {
        path: '',
        redirectTo: 'candidatos',
        pathMatch: 'full'
      },
      {
        path: 'candidatos',
        loadComponent: () => import('./features/admin/candidates/candidate-list.component').then(m => m.CandidateListComponent),
        title: 'Triagem de Candidaturas | WB Agency'
      },
      {
        path: 'candidates',
        redirectTo: 'candidatos',
        pathMatch: 'full'
      },
      {
        path: 'submissions',
        redirectTo: 'candidatos',
        pathMatch: 'full'
      },
      {
        path: 'dashboard',
        loadComponent: () => import('./features/admin/dashboard/dashboard.component').then(m => m.DashboardComponent),
        title: 'Painel de Controle | WB Agency'
      },
      {
        path: 'models',
        loadComponent: () => import('./features/admin/models-mgmt/models-mgmt.component').then(m => m.ModelsMgmtComponent),
        title: 'Gestão de Modelos | WB Agency'
      },
      {
        path: 'content',
        loadComponent: () => import('./features/admin/content-mgmt/content-mgmt.component').then(m => m.ContentMgmtComponent),
        title: 'Gestão de Conteúdo & Vídeo | WB Agency'
      }
    ]
  },

  // Fallback para rota inicial
  {
    path: '**',
    redirectTo: ''
  }
];
