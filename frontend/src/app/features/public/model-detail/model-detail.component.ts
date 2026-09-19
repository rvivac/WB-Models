import { Component, signal, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { ModelItem, ModelMediaItem } from '../../../shared/models/model.interface';
import { ModalComponent } from '../../../shared/components/modal/modal.component';

@Component({
  selector: 'app-model-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, ModalComponent],
  templateUrl: './model-detail.component.html',
  styleUrls: ['./model-detail.component.scss']
})
export class ModelDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);

  readonly activeTab = signal<'BOOK' | 'POLAROIDS' | 'COMPOSITE'>('BOOK');
  readonly selectedImage = signal<string | null>(null);

  readonly model = signal<ModelItem>({
    id: '1',
    name: 'Isabella Martins',
    slug: 'isabella-martins',
    gender: 'FEMALE',
    category: 'FASHION',
    heightCm: 179,
    weightKg: 58,
    bustChestCm: 84,
    waistCm: 60,
    hipsCm: 90,
    shoesSize: 38,
    eyesColor: 'Verdes',
    hairColor: 'Castanho Claro',
    instagramHandle: '@isabellamartins',
    bio: 'Modelo de destaque internacional com experiência em desfiles nas semanas de moda de Milão e Paris.',
    isActive: true,
    isFeatured: true,
    coverImageUrl: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=800&auto=format&fit=crop',
    compositeUrl: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=1200&auto=format&fit=crop',
    media: [
      {
        id: 'm1',
        mediaType: 'BOOK',
        fileUrl: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=800&auto=format&fit=crop',
        displayOrder: 1,
        isCover: true
      },
      {
        id: 'm2',
        mediaType: 'BOOK',
        fileUrl: 'https://images.unsplash.com/photo-1517841905240-472988babdf9?q=80&w=800&auto=format&fit=crop',
        displayOrder: 2,
        isCover: false
      },
      {
        id: 'm3',
        mediaType: 'BOOK',
        fileUrl: 'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?q=80&w=800&auto=format&fit=crop',
        displayOrder: 3,
        isCover: false
      },
      {
        id: 'm4',
        mediaType: 'POLAROID',
        fileUrl: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?q=80&w=800&auto=format&fit=crop',
        displayOrder: 1,
        isCover: false
      },
      {
        id: 'm5',
        mediaType: 'POLAROID',
        fileUrl: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=800&auto=format&fit=crop',
        displayOrder: 2,
        isCover: false
      }
    ],
    createdAt: '2026-01-10'
  });

  ngOnInit(): void {
    const slug = this.route.snapshot.paramMap.get('slug');
    if (slug) {
      // No fluxo real, consultar apiService.get<ModelItem>(`/models/${slug}`)
      this.model.update(m => ({ ...m, slug }));
    }
  }

  setTab(tab: 'BOOK' | 'POLAROIDS' | 'COMPOSITE'): void {
    this.activeTab.set(tab);
  }

  openPreview(url: string): void {
    this.selectedImage.set(url);
  }

  closePreview(): void {
    this.selectedImage.set(null);
  }

  downloadComposite(): void {
    const url = this.model().compositeUrl;
    if (url) {
      window.open(url, '_blank');
    }
  }
}
