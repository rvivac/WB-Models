import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { CandidateService } from '../../../core/services/candidate.service';
import {
  Candidate,
  CandidateStatus,
  CandidatePhoto
} from '../../../core/models/candidate.model';
import { fixUtf8, sanitizeCandidateName } from '../../../core/utils/text-sanitizer.util';

@Component({
  selector: 'app-candidate-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './candidate-list.component.html',
  styleUrls: ['./candidate-list.component.scss']
})
export class CandidateListComponent implements OnInit, OnDestroy {
  private readonly candidateService = inject(CandidateService);
  private readonly searchSubject = new Subject<string>();
  private searchSub?: Subscription;

  // Estados de Dados & Filtros (Signals)
  readonly candidates = signal<Candidate[]>([]);
  readonly selectedCandidate = signal<Candidate | null>(null);
  readonly isLoading = signal<boolean>(false);
  readonly isActionLoading = signal<boolean>(false);

  // Filtros & Paginação
  readonly activeStatusTab = signal<CandidateStatus | 'ALL'>('PENDING');
  readonly searchTerm = signal<string>('');
  readonly currentPage = signal<number>(0);
  readonly pageSize = signal<number>(12);
  readonly totalElements = signal<number>(0);
  readonly totalPages = signal<number>(0);

  // Contadores para as Abas
  readonly pendingCount = signal<number>(0);
  readonly approvedCount = signal<number>(0);
  readonly rejectedCount = signal<number>(0);

  // Estado do Modal / Drawer Lateral
  readonly activePhotoIndex = signal<number>(0);
  readonly scoutNotes = signal<string>('');
  readonly toast = signal<{ message: string; type: 'success' | 'error' | 'info' } | null>(null);

  // Lista de Abas de Triagem
  readonly statusTabs: { label: string; value: CandidateStatus | 'ALL' }[] = [
    { label: 'Pendentes', value: 'PENDING' },
    { label: 'Aprovados', value: 'APPROVED' },
    { label: 'Reprovados', value: 'REJECTED' },
    { label: 'Todos', value: 'ALL' }
  ];

  ngOnInit(): void {
    this.setupSearch();
    this.loadCandidates();
    this.refreshTabCounters();
  }

  ngOnDestroy(): void {
    this.searchSub?.unsubscribe();
  }

  private setupSearch(): void {
    this.searchSub = this.searchSubject
      .pipe(debounceTime(350), distinctUntilChanged())
      .subscribe(query => {
        this.searchTerm.set(query);
        this.currentPage.set(0);
        this.loadCandidates();
      });
  }

  onSearchInput(event: Event): void {
    const val = (event.target as HTMLInputElement).value;
    this.searchSubject.next(val);
  }

  setStatusTab(tab: CandidateStatus | 'ALL'): void {
    if (this.activeStatusTab() === tab) return;
    this.activeStatusTab.set(tab);
    this.currentPage.set(0);
    this.loadCandidates();
  }

  loadCandidates(): void {
    this.isLoading.set(true);

    this.candidateService.getCandidates({
      status: this.activeStatusTab(),
      page: this.currentPage(),
      size: this.pageSize(),
      search: this.searchTerm()
    }).subscribe({
      next: (res) => {
        this.isLoading.set(false);
        const sanitized = (res.content || []).map((c: any) => ({
          ...c,
          fullName: sanitizeCandidateName(c.fullName),
          city: fixUtf8(c.city),
          state: fixUtf8(c.state)
        }));
        this.candidates.set(sanitized);
        this.totalElements.set(res.totalElements);
        this.totalPages.set(res.totalPages);
        this.updateCountersFromLocal();
      },
      error: (err) => {
        this.isLoading.set(false);
        console.error('Erro ao consultar candidaturas:', err);
        this.showToast('Erro ao carregar dados do servidor.', 'error');
      }
    });
  }

  private refreshTabCounters(): void {
    // Carrega contadores gerais sem filtro para alimentar as pílulas das abas
    this.candidateService.getCandidates({ status: 'ALL', page: 0, size: 200 }).subscribe({
      next: (res) => {
        const list = res.content || [];
        this.pendingCount.set(list.filter(c => c.status === 'PENDING').length);
        this.approvedCount.set(list.filter(c => c.status === 'APPROVED').length);
        this.rejectedCount.set(list.filter(c => c.status === 'REJECTED').length);
      },
      error: () => {
        // Fallback silencioso
      }
    });
  }

  private updateCountersFromLocal(): void {
    // Sincroniza contador caso estejamos na aba específica
    if (this.activeStatusTab() === 'PENDING') {
      this.pendingCount.set(this.totalElements());
    } else if (this.activeStatusTab() === 'APPROVED') {
      this.approvedCount.set(this.totalElements());
    } else if (this.activeStatusTab() === 'REJECTED') {
      this.rejectedCount.set(this.totalElements());
    }
  }

  // --- Ações Rápidas (Aprovar / Reprovar com Atualização Otimista) ---

  quickApprove(candidate: Candidate, event?: Event): void {
    if (event) event.stopPropagation();
    this.applyStatusChange(candidate, 'APPROVED', 'Candidatura aprovada com sucesso!');
  }

  quickReject(candidate: Candidate, event?: Event): void {
    if (event) event.stopPropagation();
    this.applyStatusChange(candidate, 'REJECTED', 'Candidatura reprovada.');
  }

  private applyStatusChange(candidate: Candidate, newStatus: CandidateStatus, successMsg: string): void {
    const previousStatus = candidate.status;
    if (previousStatus === newStatus) return;

    // 1. Atualização Otimista Imediata na UI
    const updatedCandidate: Candidate = {
      ...candidate,
      status: newStatus,
      notes: this.scoutNotes() || candidate.notes
    };

    this.candidates.update(list => {
      // Se estamos em uma aba filtrada por status que não é 'ALL', removemos da visão atual
      if (this.activeStatusTab() !== 'ALL' && this.activeStatusTab() !== newStatus) {
        return list.filter(c => c.id !== candidate.id);
      }
      return list.map(c => c.id === candidate.id ? updatedCandidate : c);
    });

    // Ajusta contadores otimistas
    if (previousStatus === 'PENDING') this.pendingCount.update(c => Math.max(0, c - 1));
    if (previousStatus === 'APPROVED') this.approvedCount.update(c => Math.max(0, c - 1));
    if (previousStatus === 'REJECTED') this.rejectedCount.update(c => Math.max(0, c - 1));

    if (newStatus === 'PENDING') this.pendingCount.update(c => c + 1);
    if (newStatus === 'APPROVED') this.approvedCount.update(c => c + 1);
    if (newStatus === 'REJECTED') this.rejectedCount.update(c => c + 1);

    if (this.selectedCandidate()?.id === candidate.id) {
      this.selectedCandidate.set(updatedCandidate);
    }

    this.showToast(successMsg, 'success');

    // 2. Chamada ao Backend / Service
    this.candidateService.updateStatus(candidate.id, {
      status: newStatus,
      notes: updatedCandidate.notes
    }).subscribe({
      next: (serverCandidate) => {
        // Confirma dados normalizados do servidor
        this.candidates.update(list =>
          list.map(c => c.id === serverCandidate.id ? serverCandidate : c)
        );
        if (this.selectedCandidate()?.id === serverCandidate.id) {
          this.selectedCandidate.set(serverCandidate);
        }
      },
      error: (err) => {
        console.error('Falha ao atualizar status no servidor, revertendo:', err);
        this.showToast('Erro na sincronização com o servidor. Revertendo.', 'error');
        // Reverte estado
        this.candidates.update(list =>
          list.map(c => c.id === candidate.id ? { ...c, status: previousStatus } : c)
        );
        this.refreshTabCounters();
      }
    });
  }

  // --- Modal / Drawer Lateral de Avaliação Detalhada ---

  openDrawer(candidate: Candidate): void {
    this.selectedCandidate.set(candidate);
    this.activePhotoIndex.set(0);
    this.scoutNotes.set(candidate.notes || '');
  }

  closeDrawer(): void {
    this.selectedCandidate.set(null);
  }

  selectPhoto(index: number): void {
    this.activePhotoIndex.set(index);
  }

  saveScoutNotes(): void {
    const candidate = this.selectedCandidate();
    if (!candidate) return;

    this.isActionLoading.set(true);
    const notesText = this.scoutNotes();

    this.candidateService.updateStatus(candidate.id, {
      status: candidate.status,
      notes: notesText
    }).subscribe({
      next: (res) => {
        this.isActionLoading.set(false);
        this.selectedCandidate.set(res);
        this.candidates.update(list =>
          list.map(c => c.id === res.id ? res : c)
        );
        this.showToast('Notas da banca salvas com sucesso.', 'info');
      },
      error: (err) => {
        this.isActionLoading.set(false);
        console.error('Erro ao salvar observações:', err);
        this.showToast('Erro ao salvar notas do scout.', 'error');
      }
    });
  }

  promoteToElenco(): void {
    const candidate = this.selectedCandidate();
    if (!candidate) return;

    const confirmPromote = window.confirm(
      `Deseja aprovar e promover "${candidate.fullName}" ao elenco oficial de Modelos da WB Agency?\n\n` +
      `Será gerado um cadastro oficial com fotos e biometria para a agência.`
    );
    if (!confirmPromote) return;

    this.isActionLoading.set(true);

    this.candidateService.promoteToModel(candidate.id, true).subscribe({
      next: (promoted) => {
        this.isActionLoading.set(false);
        this.selectedCandidate.set(promoted);
        this.candidates.update(list =>
          list.map(c => c.id === promoted.id ? promoted : c)
        );
        this.showToast(`✨ ${promoted.fullName} foi integrado(a) ao elenco oficial de Modelos!`, 'success');
        this.refreshTabCounters();
        setTimeout(() => this.closeDrawer(), 1200);
      },
      error: (err) => {
        this.isActionLoading.set(false);
        console.error('Erro ao promover candidato:', err);
        this.showToast('Falha na promoção para modelo.', 'error');
      }
    });
  }

  // --- Paginação ---

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages()) {
      this.currentPage.set(page);
      this.loadCandidates();
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  // --- Helpers de Exibição ---

  getMainPhoto(candidate: Candidate): string {
    if (candidate.photos && candidate.photos.length > 0) {
      return candidate.photos[0].url;
    }
    return 'assets/images/hero-poster.jpg';
  }

  getPhotoTypeLabel(type: string): string {
    switch (type) {
      case 'POLAROID_ROSTO': return 'Polaroid Rosto';
      case 'POLAROID_PERFIL': return 'Polaroid Perfil';
      case 'CORPO_INTEIRO': return 'Corpo Inteiro';
      case 'COMPOSITE': return 'Composite';
      default: return 'Foto';
    }
  }

  getStatusBadgeClass(status: CandidateStatus): string {
    switch (status) {
      case 'APPROVED': return 'badge-approved';
      case 'REJECTED': return 'badge-rejected';
      case 'PENDING':
      default:
        return 'badge-pending';
    }
  }

  getStatusLabel(status: CandidateStatus): string {
    switch (status) {
      case 'APPROVED': return 'Aprovado';
      case 'REJECTED': return 'Reprovado';
      case 'PENDING':
      default:
        return 'Pendente';
    }
  }

  private showToast(message: string, type: 'success' | 'error' | 'info'): void {
    this.toast.set({ message, type });
    setTimeout(() => {
      this.toast.set(null);
    }, 4000);
  }
}
