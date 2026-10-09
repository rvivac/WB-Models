import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-home-hero',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './home-hero.component.html',
  styleUrls: ['./home-hero.component.scss']
})
export class HomeHeroComponent implements OnInit, OnDestroy {
  showSplashLogo = signal<boolean>(true);
  private timerId?: any;

  ngOnInit(): void {
    // Exibe o splash por exatamente 1 segundo e encerra
    this.timerId = setTimeout(() => {
      this.showSplashLogo.set(false);
    }, 1000);
  }

  ngOnDestroy(): void {
    if (this.timerId) {
      clearTimeout(this.timerId);
    }
  }
}
