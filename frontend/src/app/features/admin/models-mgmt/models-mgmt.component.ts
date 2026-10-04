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

    this.adminModelService.getModels(params).subscribe({
      next: (res) => {
        this.models.set(res.content);
        this.isLoading.set(false);
      },
      error: () => {
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
}
