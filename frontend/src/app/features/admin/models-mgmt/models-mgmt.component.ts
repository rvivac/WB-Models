import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ButtonComponent } from '../../../shared/components/button/button.component';

interface AdminModelRecord {
  id: string;
  name: string;
  category: string;
  heightCm: number;
  status: 'ATIVO' | 'INATIVO';
  photosCount: number;
  compositeReady: boolean;
}

@Component({
  selector: 'app-models-mgmt',
  standalone: true,
  imports: [CommonModule, RouterModule, ButtonComponent],
  templateUrl: './models-mgmt.component.html',
  styleUrls: ['./models-mgmt.component.scss']
})
export class ModelsMgmtComponent {
  readonly models = signal<AdminModelRecord[]>([
    { id: '1', name: 'Isabella Martins', category: 'FASHION', heightCm: 179, status: 'ATIVO', photosCount: 14, compositeReady: true },
    { id: '2', name: 'Gabriel Alencar', category: 'FASHION', heightCm: 188, status: 'ATIVO', photosCount: 18, compositeReady: true },
    { id: '3', name: 'Helena Vasconcelos', category: 'NEW_FACE', heightCm: 177, status: 'ATIVO', photosCount: 8, compositeReady: true },
    { id: '4', name: 'Lucas Mendes', category: 'COMMERCIAL', heightCm: 185, status: 'ATIVO', photosCount: 12, compositeReady: false },
    { id: '5', name: 'Camila Rocha', category: 'SPECIAL', heightCm: 180, status: 'ATIVO', photosCount: 16, compositeReady: true },
    { id: '6', name: 'Sophia Benitez', category: 'COMMERCIAL', heightCm: 175, status: 'INATIVO', photosCount: 6, compositeReady: false }
  ]);

  toggleStatus(model: AdminModelRecord): void {
    this.models.update(list =>
      list.map(m => m.id === model.id ? { ...m, status: m.status === 'ATIVO' ? 'INATIVO' : 'ATIVO' } : m)
    );
  }
}
