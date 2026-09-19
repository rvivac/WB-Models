import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent {
  readonly authService = inject(AuthService);

  readonly stats = signal([
    { label: 'Modelos no Casting', value: '24', change: '+2 este mês' },
    { label: 'Candidaturas Recebidas', value: '18', change: '5 aguardando triagem' },
    { label: 'Visualizações de Book', value: '4.820', change: '+18% vs mês anterior' },
    { label: 'Composites Baixados', value: '142', change: 'Diretores de casting' }
  ]);

  readonly recentCandidates = signal([
    { name: 'Mariana Rios', age: 19, height: '179 cm', city: 'São Paulo - SP', date: 'Hoje às 14:20', status: 'NOVO' },
    { name: 'Lucas Santana', age: 22, height: '187 cm', city: 'Campinas - SP', date: 'Ontem às 18:45', status: 'AVALIADO' },
    { name: 'Beatriz Vasques', age: 17, height: '176 cm', city: 'Santos - SP', date: '16/09/2026', status: 'NOVO' }
  ]);

  logout(): void {
    this.authService.logout();
  }
}
