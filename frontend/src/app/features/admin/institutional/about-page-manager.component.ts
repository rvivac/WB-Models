import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormArray, Validators, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { AboutPageService } from '../../../core/services/about-page.service';
import { AboutPage, AboutPillar } from '../../../shared/models/about-page.interface';

@Component({
  selector: 'app-about-page-manager',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './about-page-manager.component.html',
  styleUrls: ['./about-page-manager.component.scss']
})
export class AboutPageManagerComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly aboutService = inject(AboutPageService);

  isLoading = false;
  isSaving = false;
  feedbackMessage = '';
  feedbackType: 'success' | 'error' = 'success';
  lastUpdated: string | null = null;

  aboutForm: FormGroup = this.fb.group({
    title: ['', [Validators.maxLength(150)]],
    subtitle: ['', [Validators.maxLength(100)]],
    description: ['', [Validators.maxLength(1500)]],
    heroQuote: ['', [Validators.maxLength(500)]],
    manifestoTitle: ['', [Validators.maxLength(120)]],
    manifestoText: ['', [Validators.maxLength(3000)]],
    pillarsTitle: ['', [Validators.maxLength(120)]],
    pillars: this.fb.array([]),
    seo: this.fb.group({
      metaTitle: ['', [Validators.maxLength(150)]],
      metaDescription: ['', [Validators.maxLength(300)]]
    })
  });

  get pillarsArray(): FormArray {
    return this.aboutForm.get('pillars') as FormArray;
  }

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading = true;
    this.aboutService.getAdminAboutPage().subscribe({
      next: (data: AboutPage) => {
        this.populateForm(data);
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Erro ao carregar dados da página Sobre Nós:', err);
        this.showFeedback('Aviso: Erro ao carregar dados da página Sobre Nós.', 'error');
        this.isLoading = false;
      }
    });
  }

  private populateForm(data: AboutPage): void {
    this.lastUpdated = data.updatedAt || null;

    this.aboutForm.patchValue({
      title: data.title ?? '',
      subtitle: data.subtitle ?? '',
      description: data.description ?? '',
      heroQuote: data.heroQuote ?? '',
      manifestoTitle: data.manifestoTitle ?? '',
      manifestoText: data.manifestoText ?? '',
      pillarsTitle: data.pillarsTitle ?? '',
      seo: {
        metaTitle: data.seo?.metaTitle ?? '',
        metaDescription: data.seo?.metaDescription ?? ''
      }
    });

    this.pillarsArray.clear();
    const pillars = data.pillars && data.pillars.length > 0
      ? data.pillars
      : [];

    pillars.forEach((p, idx) => {
      this.pillarsArray.push(this.createPillarGroup(p.order || idx + 1, p.titulo, p.descricao));
    });
  }

  createPillarGroup(order: number, titulo = '', descricao = ''): FormGroup {
    return this.fb.group({
      order: [order],
      titulo: [titulo, [Validators.required, Validators.maxLength(120)]],
      descricao: [descricao, [Validators.required, Validators.maxLength(500)]]
    });
  }

  addPillar(): void {
    const nextOrder = this.pillarsArray.length + 1;
    this.pillarsArray.push(this.createPillarGroup(nextOrder, '', ''));
  }

  removePillar(index: number): void {
    if (this.pillarsArray.length <= 1) {
      this.showFeedback('A página deve conter ao menos um pilar institucional.', 'error');
      return;
    }
    this.pillarsArray.removeAt(index);
    this.reindexPillars();
  }

  movePillarUp(index: number): void {
    if (index <= 0) return;
    const current = this.pillarsArray.at(index);
    this.pillarsArray.removeAt(index);
    this.pillarsArray.insert(index - 1, current);
    this.reindexPillars();
  }

  movePillarDown(index: number): void {
    if (index >= this.pillarsArray.length - 1) return;
    const current = this.pillarsArray.at(index);
    this.pillarsArray.removeAt(index);
    this.pillarsArray.insert(index + 1, current);
    this.reindexPillars();
  }

  private reindexPillars(): void {
    this.pillarsArray.controls.forEach((ctrl, idx) => {
      ctrl.patchValue({ order: idx + 1 });
    });
  }

  save(): void {
    if (this.aboutForm.invalid) {
      this.aboutForm.markAllAsTouched();
      this.showFeedback('Por favor, preencha todos os campos obrigatórios assinalados.', 'error');
      return;
    }

    this.isSaving = true;
    const formVal: AboutPage = this.aboutForm.value;

    this.aboutService.updateAboutPage(formVal).subscribe({
      next: (saved: AboutPage) => {
        this.isSaving = false;
        this.lastUpdated = saved.updatedAt || new Date().toISOString();
        this.showFeedback('Textos e pilares da página Sobre Nós atualizados com sucesso!', 'success');
      },
      error: (err) => {
        this.isSaving = false;
        console.error('Erro ao salvar página Sobre Nós:', err);
        this.showFeedback('Erro ao persistir no servidor. Verifique a conexão e tente novamente.', 'error');
      }
    });
  }

  restoreDefaults(): void {
    if (confirm('Deseja restaurar os textos e pilares para a versão padrão homologada?')) {
      this.populateForm(this.aboutService.defaultAboutPage);
      this.showFeedback('Textos restaurados para os padrões institucionais. Clique em Salvar para persistir.', 'success');
    }
  }

  private showFeedback(msg: string, type: 'success' | 'error' = 'success'): void {
    this.feedbackMessage = msg;
    this.feedbackType = type;
    setTimeout(() => {
      if (this.feedbackMessage === msg) {
        this.feedbackMessage = '';
      }
    }, 4500);
  }
}
