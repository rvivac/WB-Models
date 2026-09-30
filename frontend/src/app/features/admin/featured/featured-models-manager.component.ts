import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CdkDragDrop, DragDropModule, moveItemInArray } from '@angular/cdk/drag-drop';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

export interface FeaturedModelSummary {
  id: string;
  artisticName: string;
  category: string;
  height: number;
  isStar: boolean;
  coverPhotoUrl: string;
  displayOrder?: number;
}

@Component({
  selector: 'app-featured-models-manager',
  standalone: true,
  imports: [CommonModule, FormsModule, DragDropModule],
  templateUrl: './featured-models-manager.component.html',
  styleUrls: ['./featured-models-manager.component.scss']
})
export class FeaturedModelsManagerComponent implements OnInit {
  private readonly http = inject(HttpClient);

  featuredList: FeaturedModelSummary[] = [];
  availableModels: FeaturedModelSummary[] = [];
  searchQuery = '';
  isSubmitting = false;

  get filteredAvailableModels(): FeaturedModelSummary[] {
    const featuredIds = new Set(this.featuredList.map(m => m.id));
    return this.availableModels
      .filter(m => !featuredIds.has(m.id))
      .filter(m => m.artisticName.toLowerCase().includes(this.searchQuery.toLowerCase()));
  }

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    // 1. Busca os destaques atuais ordenados
    this.http.get<any[]>(`${environment.apiUrl}/admin/featured-models`).subscribe({
      next: (res) => {
        const list: any[] = Array.isArray(res) ? res : ((res as any)?.content || []);
        if (list && list.length > 0) {
          this.featuredList = list.map((m) => this.mapModelSummary(m));
        } else {
          this.loadMockFallback();
        }
      },
      error: () => this.loadMockFallback()
    });

    // 2. Busca todos os modelos ativos disponíveis
    this.http.get<any>(`${environment.apiUrl}/admin/models?status=ACTIVE&size=100`).subscribe({
      next: (res) => {
        const list: any[] = Array.isArray(res) ? res : (res?.content || []);
        this.availableModels = list.map((m) => this.mapModelSummary(m));
      },
      error: () => {
        this.loadAvailableMockFallback();
      }
    });
  }

  onDrop(event: CdkDragDrop<FeaturedModelSummary[]>): void {
    moveItemInArray(this.featuredList, event.previousIndex, event.currentIndex);
  }

  addToFeatured(model: FeaturedModelSummary): void {
    if (this.featuredList.length < 8) {
      this.featuredList.push({
        ...model,
        displayOrder: this.featuredList.length + 1
      });
    }
  }

  removeFromFeatured(id: string): void {
    this.featuredList = this.featuredList.filter(m => m.id !== id);
  }

  saveChanges(): void {
    if (this.featuredList.length < 4 || this.featuredList.length > 8) {
      alert('A vitrine deve conter entre 4 e 8 modelos selecionados.');
      return;
    }

    this.isSubmitting = true;
    const payload = {
      items: this.featuredList.map((model, idx) => ({
        modelId: model.id,
        displayOrder: idx + 1
      }))
    };

    this.http.put(`${environment.apiUrl}/admin/featured-models`, payload).subscribe({
      next: () => {
        this.isSubmitting = false;
        alert('Curadoria da Home atualizada com sucesso!');
      },
      error: () => {
        this.isSubmitting = false;
        alert('Erro ao salvar nova vitrine da Home.');
      }
    });
  }

  private mapModelSummary(m: any): FeaturedModelSummary {
    return {
      id: m.id,
      artisticName: m.artisticName || m.stageName || 'Sem Nome',
      category: m.category || (m.gender === 'MALE' ? 'COMMERCIAL' : 'FASHION'),
      height: m.height || m.heightCm || 175,
      isStar: Boolean(m.isStar),
      coverPhotoUrl: m.coverPhotoUrl || m.primaryPhotoUrl || 'assets/images/placeholder.jpg',
      displayOrder: m.displayOrder ?? m.featuredOrder
    };
  }

  private loadMockFallback(): void {
    this.featuredList = [
      { id: '1', artisticName: 'Isabella Viana', category: 'FASHION', height: 179, isStar: true, coverPhotoUrl: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=800&auto=format&fit=crop', displayOrder: 1 },
      { id: '2', artisticName: 'Lucas Albuquerque', category: 'COMMERCIAL', height: 188, isStar: false, coverPhotoUrl: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?q=80&w=800&auto=format&fit=crop', displayOrder: 2 },
      { id: '3', artisticName: 'Beatriz Zanin', category: 'FASHION', height: 177, isStar: true, coverPhotoUrl: 'https://images.unsplash.com/photo-1517841905240-472988babdf9?q=80&w=800&auto=format&fit=crop', displayOrder: 3 },
      { id: '4', artisticName: 'Gabriel Siqueira', category: 'RUNWAY', height: 186, isStar: false, coverPhotoUrl: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?q=80&w=800&auto=format&fit=crop', displayOrder: 4 }
    ];
  }

  private loadAvailableMockFallback(): void {
    this.availableModels = [
      ...this.featuredList,
      { id: '5', artisticName: 'Camila Rocha', category: 'FASHION', height: 180, isStar: true, coverPhotoUrl: 'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?q=80&w=800&auto=format&fit=crop' },
      { id: '6', artisticName: 'Helena Vasconcelos', category: 'COMMERCIAL', height: 177, isStar: false, coverPhotoUrl: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?q=80&w=800&auto=format&fit=crop' }
    ];
  }
}
