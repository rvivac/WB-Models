import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HomeHeroComponent } from './components/home-hero/home-hero.component';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule, 
    HomeHeroComponent
  ],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent {}
