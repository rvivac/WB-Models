import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { AuditLogService, AdminAuditLogItem } from '../../../core/services/audit-log.service';

@Component({
  selector: 'app-audit-logs',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './audit-logs.component.html',
  styleUrls: ['./audit-logs.component.scss']
})
export class AuditLogsComponent implements OnInit {
  private readonly auditLogService = inject(AuditLogService);
  private readonly fb = inject(FormBuilder);

  readonly logs = signal<AdminAuditLogItem[]>([]);
  readonly totalElements = signal<number>(0);
  readonly totalPages = signal<number>(0);
  readonly currentPage = signal<number>(0);
  readonly pageSize = signal<number>(20);
  readonly isLoading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);

  // Modal de Detalhes
  readonly isDetailsModalOpen = signal<boolean>(false);
  readonly selectedLog = signal<AdminAuditLogItem | null>(null);

  // Formulário de Filtros
  readonly filterForm: FormGroup = this.fb.group({
    search: [''],
    adminEmail: [''],
    action: [''],
    resourceType: [''],
    startDate: [''],
    endDate: ['']
  });

  readonly availableActions = [
    { value: '', label: 'Todas as Ações' },
    { value: 'CREATE', label: 'Criação' },
    { value: 'UPDATE', label: 'Atualização' },
    { value: 'DELETE', label: 'Exclusão' },
    { value: 'DECISION', label: 'Triagem de Candidatura' },
    { value: 'PROMOTE', label: 'Promoção a Modelo' },
    { value: 'LOGIN', label: 'Login com Sucesso' },
    { value: 'LOGIN_FAILED', label: 'Falha de Login' },
    { value: 'LOGOUT', label: 'Logout' },
    { value: 'TOGGLE_STATUS', label: 'Alteração de Status' },
    { value: 'UPDATE_ROLE', label: 'Alteração de Papel' }
  ];

  readonly availableModules = [
    { value: '', label: 'Todos os Módulos' },
    { value: 'SCOUTING_CANDIDATE', label: 'Scouting Desk' },
    { value: 'MODEL', label: 'Casting (Modelos)' },
    { value: 'ADMIN_USER', label: 'Gestão de Usuários' },
    { value: 'INSTITUTIONAL_HOME', label: 'Hero / Home' },
    { value: 'INSTITUTIONAL_ABOUT', label: 'Sobre Nós' },
    { value: 'APPLY_FAQ', label: 'FAQ / Quero Ser Modelo' },
    { value: 'INSTITUTIONAL_CONTACT', label: 'Canais de Contato' },
    { value: 'INSTITUTIONAL_TRANSLATION', label: 'Traduções (PT/EN)' },
    { value: 'AUTH', label: 'Autenticação & Sessão' }
  ];

  ngOnInit(): void {
    this.loadLogs();
  }

  loadLogs(page = 0): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.currentPage.set(page);

    const f = this.filterForm.value;

    let isoStartDate: string | undefined = undefined;
    if (f.startDate) {
      isoStartDate = new Date(f.startDate + 'T00:00:00Z').toISOString();
    }

    let isoEndDate: string | undefined = undefined;
    if (f.endDate) {
      isoEndDate = new Date(f.endDate + 'T23:59:59Z').toISOString();
    }

    this.auditLogService.getAuditLogs({
      search: f.search || undefined,
      adminEmail: f.adminEmail || undefined,
      action: f.action || undefined,
      resourceType: f.resourceType || undefined,
      startDate: isoStartDate,
      endDate: isoEndDate,
      page: page,
      size: this.pageSize(),
      sort: 'createdAt,desc'
    }).subscribe({
      next: (response) => {
        this.logs.set(response.content || []);
        this.totalElements.set(response.totalElements || 0);
        this.totalPages.set(response.totalPages || 0);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err?.error?.detail || 'Erro ao carregar a trilha de auditoria.');
      }
    });
  }

  applyFilters(): void {
    this.loadLogs(0);
  }

  clearFilters(): void {
    this.filterForm.reset({
      search: '',
      adminEmail: '',
      action: '',
      resourceType: '',
      startDate: '',
      endDate: ''
    });
    this.loadLogs(0);
  }

  nextPage(): void {
    if (this.currentPage() + 1 < this.totalPages()) {
      this.loadLogs(this.currentPage() + 1);
    }
  }

  prevPage(): void {
    if (this.currentPage() > 0) {
      this.loadLogs(this.currentPage() - 1);
    }
  }

  openDetailsModal(log: AdminAuditLogItem): void {
    this.selectedLog.set(log);
    this.isDetailsModalOpen.set(true);
  }

  closeDetailsModal(): void {
    this.selectedLog.set(null);
    this.isDetailsModalOpen.set(false);
  }

  getActionBadgeClass(action: string): string {
    const act = (action || '').toUpperCase();
    if (act.includes('CREATE') || act.includes('PROMOTE')) {
      return 'badge-create';
    }
    if (act.includes('UPDATE') || act.includes('STATUS') || act.includes('ROLE') || act.includes('DECISION')) {
      return 'badge-update';
    }
    if (act.includes('LOGIN_FAILED') || act.includes('DELETE')) {
      return 'badge-danger';
    }
    if (act.includes('LOGIN') || act.includes('AUTH')) {
      return 'badge-auth';
    }
    return 'badge-neutral';
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '-';
    try {
      const date = new Date(dateStr);
      return date.toLocaleString('pt-BR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      });
    } catch {
      return dateStr;
    }
  }

  formatUserAgent(ua?: string): string {
    if (!ua) return 'Desconhecido';
    if (ua.includes('Chrome') && !ua.includes('Edg')) return 'Chrome';
    if (ua.includes('Safari') && !ua.includes('Chrome')) return 'Safari';
    if (ua.includes('Firefox')) return 'Firefox';
    if (ua.includes('Edg')) return 'Edge';
    return ua.length > 25 ? ua.substring(0, 25) + '...' : ua;
  }

  getInitials(email?: string): string {
    if (!email) return 'AD';
    const parts = email.split('@')[0].split('.');
    if (parts.length >= 2) {
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return email.substring(0, 2).toUpperCase();
  }

  formatJson(data: any): string {
    if (!data) return 'Nenhum detalhe adicional registrado.';
    try {
      return JSON.stringify(data, null, 2);
    } catch {
      return String(data);
    }
  }
}
