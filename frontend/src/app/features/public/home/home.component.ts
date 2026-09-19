import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { HomeHeroComponent } from './components/home-hero/home-hero.component';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';

interface FeaturedModel {
  name: string;
  category: string;
  imageUrl: string;
  height: string;
  slug: string;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterModule, HomeHeroComponent, TranslatePipe],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent {
  readonly featuredModels = signal<FeaturedModel[]>([
    {
      name: 'Isabella Martins',
      category: 'Editorial / Stars',
      imageUrl: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=800&auto=format&fit=crop',
      height: '179 cm',
      slug: 'isabella-martins'
    },
    {
      name: 'Gabriel Alencar',
      category: 'International Fashion',
      imageUrl: 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?q=80&w=800&auto=format&fit=crop',
      height: '188 cm',
      slug: 'gabriel-alencar'
    },
    {
      name: 'Helena Vasconcelos',
      category: 'New Face Star',
      imageUrl: 'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?q=80&w=800&auto=format&fit=crop',
      height: '177 cm',
      slug: 'helena-vasconcelos'
    }
  ]);
}
