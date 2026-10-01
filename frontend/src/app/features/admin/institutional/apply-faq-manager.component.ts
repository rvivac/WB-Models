import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ApplyFaqService } from '../../../core/services/apply-faq.service';
import { ApplyFaq, ApplyHeader } from '../../../shared/models/apply-faq.interface';

@Component({
  selector: 'app-apply-faq-manager',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './apply-faq-manager.component.html',
  styleUrls: ['./apply-faq-manager.component.scss']
})
export class ApplyFaqManagerComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly faqService = inject(ApplyFaqService);

  headerForm: FormGroup = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(100)]],
    subtitle: ['', [Validators.required, Validators.maxLength(100)]],
    description: ['', [Validators.required, Validators.maxLength(500)]]
  });

  faqModalForm: FormGroup = this.fb.group({
    question: ['', [Validators.required, Validators.maxLength(250)]],
    answer: ['', [Validators.required, Validators.maxLength(2000)]],
    isActive: [true]
  });

  faqs: ApplyFaq[] = [];
  isLoading = false;
  isSavingHeader = false;
  isSavingFaq = false;
  feedbackMessage = '';
  feedbackType: 'success' | 'error' = 'success';

  // Modal de Criação / Edição
  isModalOpen = false;
  editingFaqId: string | null = null;

  // Modal de Exclusão
  deleteModalOpen = false;
  faqToDelete: ApplyFaq | null = null;

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading = true;

    this.faqService.getAdminApplyHeader().subscribe({
      next: (header: ApplyHeader) => {
        if (header) {
          this.headerForm.patchValue({
            title: header.title || this.faqService.defaultHeader.title,
            subtitle: header.subtitle || this.faqService.defaultHeader.subtitle,
            description: header.description || this.faqService.defaultHeader.description
          });
        }
      },
      error: (err) => console.error('Erro ao carregar cabeçalho:', err)
    });

    this.faqService.getAdminFaqs().subscribe({
      next: (faqs: ApplyFaq[]) => {
        this.faqs = [...(faqs || [])].sort((a, b) => a.displayOrder - b.displayOrder);
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Erro ao carregar FAQs:', err);
        this.isLoading = false;
      }
    });
  }

  saveHeader(): void {
    if (this.headerForm.invalid) {
      this.headerForm.markAllAsTouched();
      return;
    }

    this.isSavingHeader = true;
    const dto: ApplyHeader = this.headerForm.value;

    this.faqService.updateApplyHeader(dto).subscribe({
      next: (res) => {
        this.isSavingHeader = false;
        this.showFeedback('Textos do cabeçalho de candidatura atualizados com sucesso!', 'success');
      },
      error: (err) => {
        this.isSavingHeader = false;
        this.showFeedback('Falha ao atualizar cabeçalho. Tente novamente.', 'error');
      }
    });
  }

  openCreateModal(): void {
    this.editingFaqId = null;
    this.faqModalForm.reset({
      question: '',
      answer: '',
      isActive: true
    });
    this.isModalOpen = true;
  }

  openEditModal(faq: ApplyFaq): void {
    this.editingFaqId = faq.id;
    this.faqModalForm.patchValue({
      question: faq.question,
      answer: faq.answer,
      isActive: faq.isActive
    });
    this.isModalOpen = true;
  }

  closeModal(): void {
    this.isModalOpen = false;
    this.editingFaqId = null;
    this.faqModalForm.reset();
  }

  saveFaqModal(): void {
    if (this.faqModalForm.invalid) {
      this.faqModalForm.markAllAsTouched();
      return;
    }

    this.isSavingFaq = true;
    const formVal = this.faqModalForm.value;

    if (this.editingFaqId) {
      this.faqService.updateFaq(this.editingFaqId, {
        question: formVal.question,
        answer: formVal.answer,
        isActive: formVal.isActive
      }).subscribe({
        next: (updated) => {
          this.isSavingFaq = false;
          this.closeModal();
          this.showFeedback('Pergunta de FAQ atualizada com sucesso!', 'success');
          this.loadData();
        },
        error: (err) => {
          this.isSavingFaq = false;
          this.showFeedback('Erro ao atualizar pergunta. Tente novamente.', 'error');
        }
      });
    } else {
      const nextOrder = this.faqs.length > 0 ? Math.max(...this.faqs.map(f => f.displayOrder)) + 1 : 0;

      this.faqService.createFaq({
        question: formVal.question,
        answer: formVal.answer,
        displayOrder: nextOrder,
        isActive: formVal.isActive
      }).subscribe({
        next: (created) => {
          this.isSavingFaq = false;
          this.closeModal();
          this.showFeedback('Nova pergunta cadastrada com sucesso!', 'success');
          this.loadData();
        },
        error: (err) => {
          this.isSavingFaq = false;
          this.showFeedback('Erro ao cadastrar pergunta. Tente novamente.', 'error');
        }
      });
    }
  }

  toggleStatus(faq: ApplyFaq): void {
    this.faqService.toggleStatus(faq.id).subscribe({
      next: (res) => {
        faq.isActive = res.isActive;
        this.showFeedback(`Status da pergunta alterado para ${res.isActive ? 'Ativo' : 'Inativo'}.`, 'success');
      },
      error: (err) => {
        this.showFeedback('Erro ao alterar status da pergunta.', 'error');
      }
    });
  }

  openDeleteModal(faq: ApplyFaq): void {
    this.faqToDelete = faq;
    this.deleteModalOpen = true;
  }

  cancelDelete(): void {
    this.deleteModalOpen = false;
    this.faqToDelete = null;
  }

  executeDelete(): void {
    if (!this.faqToDelete) return;

    const id = this.faqToDelete.id;
    this.faqService.deleteFaq(id).subscribe({
      next: () => {
        this.deleteModalOpen = false;
        this.faqToDelete = null;
        this.showFeedback('Pergunta de FAQ excluída com sucesso!', 'success');
        this.loadData();
      },
      error: (err) => {
        this.deleteModalOpen = false;
        this.faqToDelete = null;
        this.showFeedback('Erro ao excluir pergunta de FAQ.', 'error');
      }
    });
  }

  moveUp(index: number): void {
    if (index <= 0) return;
    this.swapAndPersist(index, index - 1);
  }

  moveDown(index: number): void {
    if (index >= this.faqs.length - 1) return;
    this.swapAndPersist(index, index + 1);
  }

  private swapAndPersist(indexA: number, indexB: number): void {
    const temp = this.faqs[indexA];
    this.faqs[indexA] = this.faqs[indexB];
    this.faqs[indexB] = temp;

    // Atualiza displayOrder sequencial
    this.faqs.forEach((faq, idx) => {
      faq.displayOrder = idx;
    });

    const reorderPayload = {
      items: this.faqs.map(f => ({ id: f.id, displayOrder: f.displayOrder }))
    };

    this.faqService.reorderFaqs(reorderPayload).subscribe({
      next: () => {
        this.showFeedback('Ordem das perguntas atualizada com sucesso!', 'success');
      },
      error: (err) => {
        console.error('Erro ao reordenar:', err);
      }
    });
  }

  private showFeedback(msg: string, type: 'success' | 'error' = 'success'): void {
    this.feedbackMessage = msg;
    this.feedbackType = type;
    setTimeout(() => {
      if (this.feedbackMessage === msg) {
        this.feedbackMessage = '';
      }
    }, 4000);
  }
}
