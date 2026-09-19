import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ModalComponent } from '../../../shared/components/modal/modal.component';

interface CandidateRecord {
  id: string;
  fullName: string;
  email: string;
  phone: string;
  age: number;
  gender: string;
  heightCm: number;
  bustCm: number;
  waistCm: number;
  hipsCm: number;
  city: string;
  instagram: string;
  photos: string[];
  status: 'PENDENTE' | 'APROVADO' | 'ARQUIVADO';
  createdAt: string;
}

@Component({
  selector: 'app-candidates-mgmt',
  standalone: true,
  imports: [CommonModule, RouterModule, ModalComponent],
  templateUrl: './candidates-mgmt.component.html',
  styleUrls: ['./candidates-mgmt.component.scss']
})
export class CandidatesMgmtComponent {
  readonly selectedCandidate = signal<CandidateRecord | null>(null);

  readonly candidates = signal<CandidateRecord[]>([
    {
      id: 'c1',
      fullName: 'Mariana Rios',
      email: 'mariana.rios@exemplo.com',
      phone: '(11) 98888-7777',
      age: 19,
      gender: 'Feminino',
      heightCm: 179,
      bustCm: 84,
      waistCm: 60,
      hipsCm: 90,
      city: 'São Paulo - SP',
      instagram: '@marianarios',
      photos: [
        'https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=800&auto=format&fit=crop',
        'https://images.unsplash.com/photo-1517841905240-472988babdf9?q=80&w=800&auto=format&fit=crop'
      ],
      status: 'PENDENTE',
      createdAt: '18/09/2026 14:20'
    },
    {
      id: 'c2',
      fullName: 'Lucas Santana',
      email: 'lucas.santana@exemplo.com',
      phone: '(19) 97777-5555',
      age: 22,
      gender: 'Masculino',
      heightCm: 187,
      bustCm: 98,
      waistCm: 76,
      hipsCm: 96,
      city: 'Campinas - SP',
      instagram: '@lucassantana',
      photos: [
        'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?q=80&w=800&auto=format&fit=crop'
      ],
      status: 'APROVADO',
      createdAt: '17/09/2026 18:45'
    },
    {
      id: 'c3',
      fullName: 'Beatriz Vasques',
      email: 'beatriz.vasques@exemplo.com',
      phone: '(13) 99111-2222',
      age: 17,
      gender: 'Feminino',
      heightCm: 176,
      bustCm: 82,
      waistCm: 59,
      hipsCm: 88,
      city: 'Santos - SP',
      instagram: '@beavasques',
      photos: [
        'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?q=80&w=800&auto=format&fit=crop'
      ],
      status: 'PENDENTE',
      createdAt: '16/09/2026 11:30'
    }
  ]);

  openDetails(candidate: CandidateRecord): void {
    this.selectedCandidate.set(candidate);
  }

  closeDetails(): void {
    this.selectedCandidate.set(null);
  }

  updateStatus(candidateId: string, newStatus: 'APROVADO' | 'ARQUIVADO'): void {
    this.candidates.update(list =>
      list.map(c => c.id === candidateId ? { ...c, status: newStatus } : c)
    );
    if (this.selectedCandidate()?.id === candidateId) {
      this.selectedCandidate.update(c => c ? { ...c, status: newStatus } : null);
    }
  }
}
