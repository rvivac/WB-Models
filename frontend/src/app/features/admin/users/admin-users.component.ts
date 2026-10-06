import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AdminUserService, AdminUserItem, CreateAdminUserPayload } from '../../../core/services/admin-user.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './admin-users.component.html',
  styleUrls: ['./admin-users.component.scss']
})
export class AdminUsersComponent implements OnInit {
  private readonly adminUserService = inject(AdminUserService);
  private readonly authService = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  readonly users = signal<AdminUserItem[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  // Modais
  readonly isCreateModalOpen = signal<boolean>(false);
  readonly isRoleModalOpen = signal<boolean>(false);
  readonly isSuccessPasswordModalOpen = signal<boolean>(false);
  readonly isDeleteModalOpen = signal<boolean>(false);

  // Estados temporários
  readonly createdTempPassword = signal<string>('');
  readonly createdUserEmail = signal<string>('');
  readonly selectedUserForRole = signal<AdminUserItem | null>(null);
  readonly userToDelete = signal<AdminUserItem | null>(null);
  readonly copiedPassword = signal<boolean>(false);

  // Formulário de Criação
  readonly createUserForm: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
    role: ['ADMIN', [Validators.required]]
  });

  // Formulário de Alteração de Papel
  readonly changeRoleForm: FormGroup = this.fb.group({
    role: ['ADMIN', [Validators.required]]
  });

  get currentAdminEmail(): string {
    return this.authService.currentUser()?.email || '';
  }

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.adminUserService.listUsers().subscribe({
      next: (response) => {
        this.users.set(response.content || []);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err?.error?.detail || 'Erro ao carregar lista de administradores.');
      }
    });
  }

  openCreateModal(): void {
    this.createUserForm.reset({ role: 'ADMIN' });
    this.isCreateModalOpen.set(true);
  }

  closeCreateModal(): void {
    this.isCreateModalOpen.set(false);
  }

  submitCreateUser(): void {
    if (this.createUserForm.invalid) {
      this.createUserForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    const payload: CreateAdminUserPayload = this.createUserForm.value;

    this.adminUserService.createUser(payload).subscribe({
      next: (res) => {
        this.isLoading.set(false);
        this.closeCreateModal();
        this.createdTempPassword.set(res.temporaryPassword);
        this.createdUserEmail.set(res.user.email);
        this.copiedPassword.set(false);
        this.isSuccessPasswordModalOpen.set(true);
        this.loadUsers();
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err?.error?.detail || 'Erro ao cadastrar novo administrador.');
      }
    });
  }

  closeSuccessPasswordModal(): void {
    this.isSuccessPasswordModalOpen.set(false);
    this.createdTempPassword.set('');
  }

  copyTemporaryPassword(): void {
    const pwd = this.createdTempPassword();
    if (pwd && typeof navigator !== 'undefined' && navigator.clipboard) {
      navigator.clipboard.writeText(pwd).then(() => {
        this.copiedPassword.set(true);
        setTimeout(() => this.copiedPassword.set(false), 3000);
      });
    }
  }

  openRoleModal(user: AdminUserItem): void {
    this.selectedUserForRole.set(user);
    // Se o usuario for WEBMASTER (redundancia), preenche o formulario com o valor e força disabled
    // posteriormente via HTML. Mas o ideal eh que openRoleModalSafe tenha bloqueado a abertura.
    this.changeRoleForm.patchValue({ role: user.role });
    this.isRoleModalOpen.set(true);
  }

  closeRoleModal(): void {
    this.isRoleModalOpen.set(false);
    this.selectedUserForRole.set(null);
  }

  submitChangeRole(): void {
    const user = this.selectedUserForRole();
    if (!user) return;

    const newRole = this.changeRoleForm.value.role;

    if (this.isCurrentUser(user) && (user.role === 'WEBMASTER' || user.role === 'SUPER_ADMIN') && newRole !== user.role) {
      this.errorMessage.set('Operação bloqueada: Não é permitido rebaixar seu próprio papel de Webmaster.');
      setTimeout(() => this.errorMessage.set(null), 4000);
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.adminUserService.updateRole(user.id, newRole).subscribe({
      next: (updatedUser) => {
        this.isLoading.set(false);
        this.closeRoleModal();
        this.successMessage.set(`Papel de ${updatedUser.name} atualizado para ${updatedUser.role}.`);
        setTimeout(() => this.successMessage.set(null), 4000);
        this.loadUsers();
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err?.error?.detail || 'Erro ao atualizar nível de acesso.');
      }
    });
  }

  openDeleteModal(user: AdminUserItem): void {
    if (this.isCurrentUser(user)) {
      this.errorMessage.set('Por segurança, você não pode excluir seu próprio usuário logado.');
      setTimeout(() => this.errorMessage.set(null), 4000);
      return;
    }
    this.userToDelete.set(user);
    this.isDeleteModalOpen.set(true);
  }

  closeDeleteModal(): void {
    this.isDeleteModalOpen.set(false);
    this.userToDelete.set(null);
  }

  confirmDeleteUser(): void {
    const user = this.userToDelete();
    if (!user) return;

    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.adminUserService.deleteUser(user.id).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.closeDeleteModal();
        this.successMessage.set(`Administrador ${user.name} excluído com sucesso.`);
        setTimeout(() => this.successMessage.set(null), 4000);
        this.loadUsers();
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err?.error?.detail || 'Erro ao excluir administrador.');
      }
    });
  }

  toggleStatus(user: AdminUserItem): void {
    if (this.isCurrentUser(user)) {
      this.errorMessage.set('Por segurança, você não pode desativar seu próprio usuário logado.');
      setTimeout(() => this.errorMessage.set(null), 4000);
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.adminUserService.toggleStatus(user.id).subscribe({
      next: (updatedUser: AdminUserItem) => {
        this.isLoading.set(false);
        const action = updatedUser.isActive ? 'ativada' : 'desativada';
        this.successMessage.set(`Conta de ${updatedUser.name} foi ${action} com sucesso.`);
        setTimeout(() => this.successMessage.set(null), 4000);
        this.loadUsers();
      },
      error: (err: any) => {
        this.isLoading.set(false);
        this.errorMessage.set(err?.error?.detail || 'Erro ao alterar status do usuário.');
      }
    });
  }

  getInitials(name: string): string {
    if (!name) return 'WB';
    const parts = name.trim().split(/\s+/);
    if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }

  isCurrentUser(user: AdminUserItem): boolean {
    return user.email.toLowerCase() === this.currentAdminEmail.toLowerCase();
  }

  // ============================================================
  // 🔥 HELPERS DE BLINDAGEM RBAC-018 para papel WEBMASTER (fixo/imutavel)
  // ============================================================
  isWebmasterRole(role: string | null | undefined): boolean {
    return String(role || '').toUpperCase() === 'WEBMASTER';
  }

  /**
   * Abertura do modal de alteracao de papel. Para WEBMASTER, o papel eh FIXO e nao
   * pode ser alterado. Exibe erro e NAO abre o modal (economiza roundtrip pro backend
   * falhar com 403 e melhora UX).
   */
  openRoleModalSafe(user: AdminUserItem): void {
    if (this.isWebmasterRole(user.role)) {
      this.errorMessage.set('🔒 Operação bloqueada: O papel de WEBMASTER é fixo e não pode ser alterado (RBAC-018).');
      setTimeout(() => this.errorMessage.set(null), 6000);
      return;
    }
    this.openRoleModal(user);
  }

  /**
   * Abertura do modal de exclusao. Contas de WEBMASTER sao de provisionamento raiz
   * e NAO PODEM ser excluidas pelo painel.
   */
  openDeleteModalSafe(user: AdminUserItem): void {
    if (this.isCurrentUser(user)) {
      this.errorMessage.set('Por segurança, você não pode excluir seu próprio usuário logado.');
      setTimeout(() => this.errorMessage.set(null), 4000);
      return;
    }
    if (this.isWebmasterRole(user.role)) {
      this.errorMessage.set('🔒 Operação bloqueada: Contas de WEBMASTER são de provisionamento raiz e não podem ser excluídas pelo painel (RBAC-018).');
      setTimeout(() => this.errorMessage.set(null), 6000);
      return;
    }
    this.openDeleteModal(user);
  }

  /**
   * Submit de alteracao de papel. Blindagens locais (alem das do backend)
   * para nao enviar requisicao que ja sabemos que sera bloqueada.
   */
  submitChangeRoleSafe(): void {
    const user = this.selectedUserForRole();
    if (!user) return;

    // Defesa 1: NAO alterar papel DE UM webmaster existente (redundancia, ja que openRoleModalSafe bloqueia)
    if (this.isWebmasterRole(user.role)) {
      this.errorMessage.set('🔒 Operação bloqueada: O papel de WEBMASTER é fixo e não pode ser alterado.');
      setTimeout(() => this.errorMessage.set(null), 6000);
      return;
    }

    const newRole = String(this.changeRoleForm.value?.role || '').toUpperCase();

    // Defesa 2: NAO promover ninguem PARA webmaster (cargo maximo manual = SUPER_ADMIN)
    if (this.isWebmasterRole(newRole)) {
      this.errorMessage.set('🔒 Operação bloqueada: Não é permitido promover administradores para WEBMASTER pelo painel. Cargo máximo permitido: SUPER_ADMIN.');
      setTimeout(() => this.errorMessage.set(null), 6000);
      return;
    }

    // Resto da validacao (auto-rebaixamento) e submit
    this.submitChangeRole();
  }

  /**
   * Submit de criacao de admin. Blindagem: caso o operador tente selecionar WEBMASTER
   * no select (mesmo que o frontend oculte), faz downgrade automatico para SUPER_ADMIN
   * e exibe aviso.
   */
  submitCreateUserSafe(): void {
    if (this.createUserForm.invalid) {
      this.createUserForm.markAllAsTouched();
      return;
    }
    const formRole = String(this.createUserForm.value?.role || '').toUpperCase();
    if (this.isWebmasterRole(formRole)) {
      console.warn('[admin/users] Tentativa de CRIAR admin com role=WEBMASTER bloqueada localmente. Efetuando downgrade para SUPER_ADMIN.');
      this.successMessage.set('🔒 Aviso: Não é permitido criar contas de WEBMASTER pelo painel. O novo administrador foi promovido para o cargo máximo SUPER_ADMIN.');
      setTimeout(() => this.successMessage.set(null), 7000);
      this.createUserForm.patchValue({ role: 'SUPER_ADMIN' }, { emitEvent: false });
    }
    this.submitCreateUser();
  }
}
