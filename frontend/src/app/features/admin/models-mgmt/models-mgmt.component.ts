import { Component, OnInit, inject, signal, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AdminModelService } from '../../../core/services/admin-model.service';
import {
  AdminModelFilterParams,
  AdminModelPageResponse,
  ModelAdminItem,
  ModelGender
} from '../../../shared/models/admin-model.interface';
import { ModelFormComponent } from './model-form/model-form.component';

@Component({
  selector: 'app-models-mgmt',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, ModelFormComponent],
  templateUrl: './models-mgmt.component.html',
  styleUrls: ['./models-mgmt.component.scss']
})
export class ModelsMgmtComponent implements OnInit {
  private readonly adminModelService = inject(AdminModelService);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly models = signal<ModelAdminItem[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly isFormOpen = signal<boolean>(false);
  readonly editingModelId = signal<string | null>(null);

  // Paginação e totais (debug do envelope Spring Page)
  readonly totalElements = signal<number>(0);
  readonly totalPages = signal<number>(0);
  readonly pageSize = signal<number>(20);
  readonly page = signal<number>(0);

  readonly isConfirmDeleteOpen = signal<boolean>(false);
  readonly pendingDeleteModel = signal<ModelAdminItem | null>(null);
  readonly isDeleting = signal<boolean>(false);

  readonly filterGender = signal<ModelGender | 'ALL'>('ALL');
  readonly filterStar = signal<boolean | null>(null);
  readonly filterStatus = signal<boolean | null>(null);
  readonly searchTerm = signal<string>('');

  readonly toast = signal<{ message: string; type: 'success' | 'error' } | null>(null);
  readonly brokenPhotoIds = signal<Set<string>>(new Set());

  onThumbError(modelId: string): void {
    if (!modelId) return;
    this.brokenPhotoIds.update(s => new Set(s).add(modelId));
  }

  hasBrokenThumb(modelId: string): boolean {
    return !modelId || this.brokenPhotoIds().has(modelId);
  }

  ngOnInit(): void {
    this.loadModels();
  }

  loadModels(): void {
    this.isLoading.set(true);
    const params: AdminModelFilterParams = {
      page: this.page(),
      size: this.pageSize(),
      gender: this.filterGender(),
      isStar: this.filterStar(),
      isActive: this.filterStatus(),
      search: this.searchTerm()
    };

    console.log('%c[models-mgmt] loadModels() iniciado com params:', 'color:#4B584E;font-weight:bold', params);

    this.adminModelService.getModels(params).subscribe({
      next: (page: AdminModelPageResponse) => {
        console.groupCollapsed('%c[models-mgmt] Resposta recebida do service', 'color:#4B584E;font-weight:bold');
        console.log('page obj completo:', page);
        console.log('page.content (lista final a renderizar):', page.content);
        console.log('page.content.length =', page.content?.length ?? 0);
        console.log('page.totalElements =', page.totalElements);
        console.log('page.totalPages    =', page.totalPages);
        console.log('page.size          =', page.size);
        console.log('page.number (page index) =', page.number);
        console.groupEnd();

        // ⚠️ Garantia EXTRA: se veio array puro (endpoint nao paginado), normaliza:
        const list: ModelAdminItem[] = Array.isArray(page)
          ? page as any
          : Array.isArray(page?.content)
            ? page.content
            : [];

        const totalEl: number = (page as any)?.totalElements ?? list.length ?? 0;
        const totalPg: number = (page as any)?.totalPages ?? Math.max(1, Math.ceil(totalEl / (this.pageSize() || 20)));

        console.log('[models-mgmt] ✅ FINAL -> list.length=', list.length, ' totalElements=', totalEl, 'totalPages=', totalPg);

        this.models.set(list || []);
        this.totalElements.set(totalEl);
        this.totalPages.set(totalPg);
        this.isLoading.set(false);

        // ⚠️ Força detecção de mudanças (corrige problema de tabela "invisivel" por
        // zona do Angular / Signals + RxJS pipe async)
        this.cdr.markForCheck();
        try { this.cdr.detectChanges(); } catch { /* standalone componentes nao tem ViewContainer as vezes */ }
      },
      error: (err) => {
        console.error('%c[models-mgmt] 🔴 ERRO ao carregar modelos:', 'color:#B91C1C;font-weight:bold', err);
        this.models.set([]);
        this.totalElements.set(0);
        this.totalPages.set(0);
        this.isLoading.set(false);
        this.cdr.markForCheck();
        const detail = (err as any)?.error?.detail || (err as any)?.message || '';
        this.showToast(`Erro ao carregar catálogo de modelos. ${detail ? 'Detalhe: ' + detail : ''}`, 'error');
      }
    });
  }

  openCreateForm(): void {
    this.editingModelId.set(null);
    this.isFormOpen.set(true);
  }

  openEditForm(model: ModelAdminItem): void {
    this.editingModelId.set(model.id);
    this.isFormOpen.set(true);
  }

  closeForm(): void {
    this.isFormOpen.set(false);
    this.editingModelId.set(null);
  }

  onModelSaved(savedModel: ModelAdminItem): void {
    this.closeForm();
    this.loadModels();
    this.showToast(`Modelo "${savedModel.stageName}" salvo com sucesso!`, 'success');
  }

  toggleStatus(model: ModelAdminItem): void {
    const nextStatus = !model.isActive;
    this.adminModelService.updateStatus(model.id, nextStatus).subscribe({
      next: (updated) => {
        this.models.update((list) =>
          list.map((m) => (m.id === model.id ? { ...m, isActive: updated.isActive } : m))
        );
        this.showToast(
          `Status de "${model.stageName}" alterado para ${nextStatus ? 'Ativo' : 'Inativo'}.`,
          'success'
        );
      },
      error: () => this.showToast('Erro ao atualizar status do modelo.', 'error')
    });
  }

  toggleStar(model: ModelAdminItem): void {
    const nextStar = !model.isStar;
    this.adminModelService.updateStar(model.id, nextStar).subscribe({
      next: (updated) => {
        this.models.update((list) =>
          list.map((m) => (m.id === model.id ? { ...m, isStar: updated.isStar } : m))
        );
        this.showToast(
          `Modelo "${model.stageName}" ${nextStar ? 'adicionado às Stars ★' : 'removido das Stars'}.`,
          'success'
        );
      },
      error: () => this.showToast('Erro ao atualizar classificação Star.', 'error')
    });
  }

  setGenderFilter(gender: ModelGender | 'ALL'): void {
    this.filterGender.set(gender);
    this.loadModels();
  }

  setStarFilter(val: boolean | null): void {
    this.filterStar.set(val);
    this.loadModels();
  }

  showToast(message: string, type: 'success' | 'error'): void {
    this.toast.set({ message, type });
    setTimeout(() => {
      this.toast.set(null);
    }, 4000);
  }

  // ============================================================
  // 🗑️ FUNÇÃO APAGAR MODELO - CONFIRMAÇÃO 2 CLIQUES
  // ============================================================

  /**
   * 1º clique: Abre modal de confirmação (PASSO 1).
   */
  confirmDelete(model: ModelAdminItem): void {
    if (!model || !model.id) return;
    this.pendingDeleteModel.set(model);
    this.isConfirmDeleteOpen.set(true);
  }

  /**
   * Fecha modal sem apagar (clique cancelar, X, ou backdrop).
   */
  cancelDelete(): void {
    this.isConfirmDeleteOpen.set(false);
    this.pendingDeleteModel.set(null);
    this.isDeleting.set(false);
  }

  /**
   * 2º clique: Confirma exclusão PERMANENTE (PASSO 2).
   * Chama backend DELETE /admin/models/{id} via AdminModelService.deleteModel().
   *
   * Segurança extra: só executa se pendingDeleteModel estiver definido
   * (evita duplo clique ou exclusao acidental).
   */
  executeDelete(): void {
    const target = this.pendingDeleteModel();
    if (!target || !target.id || this.isDeleting()) return;

    this.isDeleting.set(true);
    this.adminModelService.deleteModel(target.id).subscribe({
      next: () => {
        // (A) Remove o modelo da lista LOCAL imediatamente (UI fica responsiva)
        this.models.update((list) => list.filter((m) => m.id !== target.id));
        // (B) Recarrega backend para confirmar paginacao e totais sincronizados
        this.loadModels();
        this.showToast(`Modelo "${target.stageName}" foi excluído permanentemente com sucesso.`, 'success');
        this.cancelDelete();
      },
      error: (err) => {
        console.error('[Apagar Modelo] Falha DELETE backend:', err);
        const detail = (err as any)?.error?.detail || (err as any)?.message || '';
        this.showToast(
          `Erro ao excluir modelo "${target.stageName}". ${detail ? ` Detalhe: ${detail}` : 'Tente novamente em instantes.'}`,
          'error'
        );
        this.isDeleting.set(false);
      }
    });
  }
}
