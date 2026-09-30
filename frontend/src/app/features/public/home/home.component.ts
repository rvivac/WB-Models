import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HomeHeroComponent } from './components/home-hero/home-hero.component';
import { HomeFeaturedModelsComponent } from './components/home-featured-models/home-featured-models.component';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule, 
    HomeHeroComponent,
    HomeFeaturedModelsComponent
  ],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent {}
