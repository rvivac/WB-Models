import { Component, signal, computed, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { ModelItem } from '../../../shared/models/model.interface';

@Component({
  selector: 'app-models',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './models.component.html',
  styleUrls: ['./models.component.scss']
})
export class ModelsComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);

  readonly selectedCategory = signal<string>('ALL');

  readonly models = signal<ModelItem[]>([
    {
      id: '1',
      name: 'Isabella Martins',
      slug: 'isabella-martins',
      gender: 'FEMALE',
      category: 'FASHION',
      heightCm: 179,
      bustChestCm: 84,
      waistCm: 60,
      hipsCm: 90,
      shoesSize: 38,
      eyesColor: 'Verdes',
      hairColor: 'Castanho Claro',
      isActive: true,
      isFeatured: true,
      coverImageUrl: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=800&auto=format&fit=crop',
      media: [],
      createdAt: '2026-01-10'
    },
    {
      id: '2',
      name: 'Gabriel Alencar',
      slug: 'gabriel-alencar',
      gender: 'MALE',
      category: 'FASHION',
      heightCm: 188,
      bustChestCm: 98,
      waistCm: 76,
      hipsCm: 96,
      shoesSize: 42,
      eyesColor: 'Castanhos',
      hairColor: 'Preto',
      isActive: true,
      isFeatured: true,
      coverImageUrl: 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?q=80&w=800&auto=format&fit=crop',
      media: [],
      createdAt: '2026-01-12'
    },
    {
      id: '3',
      name: 'Helena Vasconcelos',
      slug: 'helena-vasconcelos',
      gender: 'FEMALE',
      category: 'NEW_FACE',
      heightCm: 177,
      bustChestCm: 82,
      waistCm: 59,
      hipsCm: 89,
      shoesSize: 37,
      eyesColor: 'Azuis',
      hairColor: 'Loiro',
      isActive: true,
      isFeatured: true,
      coverImageUrl: 'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?q=80&w=800&auto=format&fit=crop',
      media: [],
      createdAt: '2026-02-01'
    },
    {
      id: '4',
      name: 'Lucas Mendes',
      slug: 'lucas-mendes',
      gender: 'MALE',
      category: 'COMMERCIAL',
      heightCm: 185,
      bustChestCm: 96,
      waistCm: 78,
      hipsCm: 98,
      shoesSize: 41,
      eyesColor: 'Verdes',
      hairColor: 'Castanho',
      isActive: true,
      isFeatured: false,
      coverImageUrl: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?q=80&w=800&auto=format&fit=crop',
      media: [],
      createdAt: '2026-02-15'
    },
    {
      id: '5',
      name: 'Camila Rocha',
      slug: 'camila-rocha',
      gender: 'FEMALE',
      category: 'SPECIAL',
      heightCm: 180,
      bustChestCm: 86,
      waistCm: 62,
      hipsCm: 92,
      shoesSize: 39,
      eyesColor: 'Castanhos',
      hairColor: 'Castanho Escuro',
      isActive: true,
      isFeatured: false,
      coverImageUrl: 'https://images.unsplash.com/photo-1517841905240-472988babdf9?q=80&w=800&auto=format&fit=crop',
      media: [],
      createdAt: '2026-03-01'
    },
    {
      id: '6',
      name: 'Sophia Benitez',
      slug: 'sophia-benitez',
      gender: 'FEMALE',
      category: 'COMMERCIAL',
      heightCm: 175,
      bustChestCm: 85,
      waistCm: 61,
      hipsCm: 91,
      shoesSize: 37,
      eyesColor: 'Mel',
      hairColor: 'Castanho',
      isActive: true,
      isFeatured: false,
      coverImageUrl: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?q=80&w=800&auto=format&fit=crop',
      media: [],
      createdAt: '2026-03-05'
    }
  ]);

  // Filtragem reativa com Signal computed
  readonly filteredModels = computed(() => {
    const filter = this.selectedCategory();
    if (filter === 'ALL') {
      return this.models();
    }
    return this.models().filter(m =>
      m.gender === filter || m.category === filter
    );
  });

  ngOnInit(): void {
    const path = this.route.snapshot.url.map(s => s.path).join('/');
    if (path.includes('female')) {
      this.selectedCategory.set('FEMALE');
    } else if (path.includes('male')) {
      this.selectedCategory.set('MALE');
    } else if (path.includes('stars')) {
      this.selectedCategory.set('SPECIAL');
    }

    this.route.queryParams.subscribe(params => {
      if (params['category']) {
        this.selectedCategory.set(params['category']);
      } else if (params['gender']) {
        this.selectedCategory.set(params['gender']);
      }
    });
  }

  setFilter(category: string): void {
    this.selectedCategory.set(category);
  }
}
