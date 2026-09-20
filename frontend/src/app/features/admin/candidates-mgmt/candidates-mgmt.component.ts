import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { AdminCandidateService } from '../../../core/services/admin-candidate.service';
import {
  CandidateSubmissionResponse,
  SubmissionStatus
} from '../../../shared/models/candidate-submission.interface';

@Component({
  selector: 'app-candidates-mgmt',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './candidates-mgmt.component.html',
  styleUrls: ['./candidates-mgmt.component.scss']
})
export class CandidatesMgmtComponent implements OnInit, OnDestroy {
  private readonly adminCandidateService = inject(AdminCandidateService);
  private readonly searchSubject = new Subject<string>();
  private searchSub?: Subscription;

  // Estados de Dados & Filtros
  readonly submissions = signal<CandidateSubmissionResponse[]>([]);
  readonly selectedSubmission = signal<CandidateSubmissionResponse | null>(null);
  readonly isLoading = signal<boolean>(false);
  readonly isUpdating = signal<boolean>(false);
  readonly isPromoting = signal<boolean>(false);

  // Paginação & Busca
  readonly searchTerm = signal<string>('');
  readonly selectedStatus = signal<SubmissionStatus | 'ALL'>('ALL');
  readonly currentPage = signal<number>(0);
  readonly pageSize = signal<number>(20);
  readonly totalElements = signal<number>(0);
  readonly totalPages = signal<number>(0);

  // Dossiê / Drawer
  readonly feedbackNotes = signal<string>('');
  readonly activePhotoIndex = signal<number>(0);
  readonly toastMessage = signal<{ text: string; type: 'success' | 'error' } | null>(null);

  readonly statusTabs: { label: string; value: SubmissionStatus | 'ALL' }[] = [
    { label: 'Todos', value: 'ALL' },
    { label: 'Pendentes', value: 'PENDING' },
    { label: 'Em Análise', value: 'REVIEWING' },
    { label: 'Contatados', value: 'CONTACTED' },
    { label: 'Aprovados', value: 'APPROVED' },
    { label: 'Rejeitados', value: 'REJECTED' },
    { label: 'Arquivados', value: 'ARCHIVED' }
  ];

  ngOnInit(): void {
    this.setupSearchDebounce();
    this.loadSubmissions();
  }

  ngOnDestroy(): void {
    this.searchSub?.unsubscribe();
  }

  private setupSearchDebounce(): void {
    this.searchSub = this.searchSubject
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(term => {
        this.searchTerm.set(term);
        this.currentPage.set(0);
        this.loadSubmissions();
      });
  }

  onSearch(term: string): void {
    this.searchSubject.next(term);
  }

  setStatusFilter(status: SubmissionStatus | 'ALL'): void {
    this.selectedStatus.set(status);
    this.currentPage.set(0);
    this.loadSubmissions();
  }

  onPageSizeChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.pageSize.set(Number(select.value));
    this.currentPage.set(0);
    this.loadSubmissions();
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages()) {
      this.currentPage.set(page);
      this.loadSubmissions();
    }
  }

  loadSubmissions(): void {
    this.isLoading.set(true);

    this.adminCandidateService.getSubmissions({
      search: this.searchTerm(),
      status: this.selectedStatus(),
      page: this.currentPage(),
      size: this.pageSize(),
      sort: 'createdAt,desc'
    }).subscribe({
      next: (page) => {
        this.isLoading.set(false);
        this.submissions.set(page.content);
        this.totalElements.set(page.totalElements);
        this.totalPages.set(page.totalPages);
      },
      error: (err) => {
        this.isLoading.set(false);
        console.error('Erro ao carregar candidaturas:', err);
        this.showToast('Falha ao carregar candidaturas do servidor.', 'error');
      }
    });
  }

  openInspection(sub: CandidateSubmissionResponse): void {
    this.selectedSubmission.set(sub);
    this.feedbackNotes.set(sub.feedbackNotes || '');
    this.activePhotoIndex.set(0);
  }

  closeInspection(): void {
    this.selectedSubmission.set(null);
  }

  selectPhoto(index: number): void {
    this.activePhotoIndex.set(index);
  }

  getCurrentPhotos(sub: CandidateSubmissionResponse): { label: string; url: string }[] {
    const photos: { label: string; url: string }[] = [];
    if (sub.facePhotoUrl) {
      photos.push({ label: 'Foto de Rosto', url: sub.facePhotoUrl });
    }
    if (sub.profilePhotoUrl) {
      photos.push({ label: 'Foto de Perfil', url: sub.profilePhotoUrl });
    }
    if (sub.fullBodyPhotoUrl) {
      photos.push({ label: 'Foto de Corpo Inteiro', url: sub.fullBodyPhotoUrl });
    }
    return photos;
  }

  updateStatus(newStatus: SubmissionStatus): void {
    const sub = this.selectedSubmission();
    if (!sub) return;

    this.isUpdating.set(true);

    this.adminCandidateService.updateStatus(sub.id, {
      status: newStatus,
      feedbackNotes: this.feedbackNotes()
    }).subscribe({
      next: (updated) => {
        this.isUpdating.set(false);
        this.selectedSubmission.set(updated);

        // Atualiza na listagem local
        this.submissions.update(list =>
          list.map(item => item.id === updated.id ? updated : item)
        );

        const statusLabels: Record<SubmissionStatus, string> = {
          APPROVED: 'aprovada',
          REJECTED: 'rejeitada',
          ARCHIVED: 'arquivada',
          REVIEWING: 'colocada em análise',
          CONTACTED: 'marcada como contatada',
          PENDING: 'revertida para pendente'
        };

        const actionText = statusLabels[newStatus] || 'atualizada';
        this.showToast(`Candidatura de ${updated.fullName} ${actionText} com sucesso!`, 'success');
      },
      error: (err) => {
        this.isUpdating.set(false);
        console.error('Erro ao atualizar status da candidatura:', err);
        const msg = err.error?.message || 'Erro ao atualizar status. Tente novamente.';
        this.showToast(msg, 'error');
      }
    });
  }

  promoteCandidate(candidate: CandidateSubmissionResponse): void {
    if (!candidate || candidate.convertedToModelId) return;

    const confirmed = window.confirm(
      `Deseja promover ${candidate.fullName} ao elenco oficial de Modelos?\n\n` +
      `Será gerado um novo perfil de Modelo com biometria completa e fotos de polaroid/book.`
    );
    if (!confirmed) return;

    this.isPromoting.set(true);

    this.adminCandidateService.promoteToModel(candidate.id, true).subscribe({
      next: (updated) => {
        this.isPromoting.set(false);
        this.selectedSubmission.set(updated);

        // Atualiza na listagem local
        this.submissions.update(list =>
          list.map(item => item.id === updated.id ? updated : item)
        );

        this.showToast(`✨ ${updated.fullName} foi promovido(a) a Modelo Oficial com sucesso!`, 'success');
        setTimeout(() => {
          this.closeInspection();
        }, 1200);
      },
      error: (err) => {
        this.isPromoting.set(false);
        console.error('Erro ao converter candidato para modelo:', err);
        const msg = err.error?.message || 'Falha ao promover candidato para modelo.';
        this.showToast(msg, 'error');
      }
    });
  }

  getStatusClass(status: SubmissionStatus | undefined): string {
    switch (status) {
      case 'APPROVED':
        return 'badge-approved';
      case 'REJECTED':
        return 'badge-rejected';
      case 'ARCHIVED':
        return 'badge-archived';
      case 'REVIEWING':
        return 'badge-reviewing';
      case 'CONTACTED':
        return 'badge-contacted';
      case 'PENDING':
      default:
        return 'badge-pending';
    }
  }

  getStatusLabel(status: SubmissionStatus | undefined): string {
    switch (status) {
      case 'APPROVED':
        return 'Aprovado';
      case 'REJECTED':
        return 'Rejeitado';
      case 'ARCHIVED':
        return 'Arquivado';
      case 'REVIEWING':
        return 'Em Análise';
      case 'CONTACTED':
        return 'Contatado';
      case 'PENDING':
      default:
        return 'Pendente';
    }
  }

  private showToast(text: string, type: 'success' | 'error'): void {
    this.toastMessage.set({ text, type });
    setTimeout(() => {
      this.toastMessage.set(null);
    }, 4500);
  }
}
