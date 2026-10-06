import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AdminModelService } from '../../../core/services/admin-model.service';
import {
  AdminModelFilterParams,
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

  readonly models = signal<ModelAdminItem[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly isFormOpen = signal<boolean>(false);
  readonly editingModelId = signal<string | null>(null);

  // ============================================================
  // 🗑️ MODAL DE CONFIRMAÇÃO: APAGAR MODELO
  // ============================================================
  readonly isConfirmDeleteOpen = signal<boolean>(false);
  readonly pendingDeleteModel = signal<ModelAdminItem | null>(null);
  readonly isDeleting = signal<boolean>(false);

  // Filtros
  readonly filterGender = signal<ModelGender | 'ALL'>('ALL');
  readonly filterStar = signal<boolean | null>(null);
  readonly filterStatus = signal<boolean | null>(null);
  readonly searchTerm = signal<string>('');

  // Toast
  readonly toast = signal<{ message: string; type: 'success' | 'error' } | null>(null);

  /**
   * IDs dos modelos cuja miniatura de foto falhou ao carregar.
   * O fallback sera exibido (quadrado "WB") em vez do icone de imagem quebrada.
   * Resolve bug: fotos apareciam no Publico, mas apareciam quebradas no Admin.
   */
  readonly brokenPhotoIds = signal<Set<string>>(new Set());

  /** Marca ID do modelo como teve erro de foto, forca placeholder cinza. */
  onThumbError(modelId: string): void {
    if (!modelId) return;
    this.brokenPhotoIds.update(s => new Set(s).add(modelId));
  }

  /** @returns true se a miniatura deve pular o <img> e usar fallback "WB". */
  hasBrokenThumb(modelId: string): boolean {
    return !modelId || this.brokenPhotoIds().has(modelId);
  }

  ngOnInit(): void {
    this.loadModels();
  }

  loadModels(): void {
    this.isLoading.set(true);
    const params: AdminModelFilterParams = {
      gender: this.filterGender(),
      isStar: this.filterStar(),
      isActive: this.filterStatus(),
      search: this.searchTerm()
    };

    console.log('[admin/models-mgmt] Chamando adminModelService.getModels com params:', params);

    this.adminModelService.getModels(params).subscribe({
      next: (res: any) => {
        console.log('[admin/models-mgmt] Dados recebidos (res):', res);

        // -------------------------------------------------------------
        // Extracao TOLERANTE a MULTIPLOS formatos de resposta
        // Evita tela vazia mesmo que ApiService empacote em { data: Page }
        // ou Spring Data retorne PageImpl { content, totalElements, ... }
        // -------------------------------------------------------------
        let list: any[] = [];

        if (Array.isArray(res)) {
          list = res;
        } else if (res && Array.isArray(res.content)) {
          list = res.content;
        } else if (res?.data && Array.isArray(res.data)) {
          list = res.data;
        } else if (res?.data?.content && Array.isArray(res.data.content)) {
          list = res.data.content;
        } else if (res?.body && Array.isArray(res.body)) {
          list = res.body;
        } else if (res?.body?.content && Array.isArray(res.body.content)) {
          list = res.body.content;
        } else if (res?.payload && Array.isArray(res.payload)) {
          list = res.payload;
        } else if (res?.payload?.content && Array.isArray(res.payload.content)) {
          list = res.payload.content;
        } else if (res?.items && Array.isArray(res.items)) {
          list = res.items;
        } else if (res?.records && Array.isArray(res.records)) {
          list = res.records;
        } else {
          list = [];
          console.warn('[admin/models-mgmt] NAO FOI POSSIVEL EXTRAIR array de itens da resposta. Estrutura recebida:',
            Object.keys(res || {}), res);
        }

        console.log(`[admin/models-mgmt] Lista extraída: ${list.length} itens`);
        this.models.set(list || []);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('[admin/models-mgmt] ERRO HTTP ao carregar modelos:', err);
        this.models.set([]);
        this.isLoading.set(false);
        this.showToast('Erro ao carregar catálogo de modelos.', 'error');
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
