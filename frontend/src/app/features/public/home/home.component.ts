import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { HomeHeroComponent } from './components/home-hero/home-hero.component';
import { HomeFeaturedModelsComponent } from './components/home-featured-models/home-featured-models.component';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule, 
    RouterModule, 
    HomeHeroComponent, 
    HomeFeaturedModelsComponent, 
    TranslatePipe
  ],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent {}
