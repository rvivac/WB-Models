import { Injectable, inject } from '@angular/core';
import { Observable, catchError, of } from 'rxjs';
import { ApiService } from './api.service';
import { ApplyFaq, ApplyFaqCreateUpdate, ApplyFaqReorder, ApplyHeader } from '../../shared/models/apply-faq.interface';

@Injectable({
  providedIn: 'root'
})
export class ApplyFaqService {
  private readonly api = inject(ApiService);

  public readonly defaultHeader: ApplyHeader = {
    title: 'Quero ser Modelo',
    subtitle: 'WB SCOUTING DESK',
    description: 'Se você deseja fazer parte do casting da WB Agency, atenção para as informações abaixo: preencha o formulário e envie suas fotos para realizarmos a avaliação digital.'
  };

  public readonly defaultFaqs: ApplyFaq[] = [
    {
      id: '00000000-0000-0000-0000-000000000001',
      question: 'Existe algum custo para inscrição ou avaliação?',
      answer: 'Não. A WB Agency nunca cobra nenhuma taxa para inscrição, avaliação de perfil, teste de vídeo ou agenciamento inicial. O processo de scouting é 100% gratuito e desconfie de qualquer abordagem cobrando taxas em nosso nome.',
      displayOrder: 0,
      isActive: true
    },
    {
      id: '00000000-0000-0000-0000-000000000002',
      question: 'Como devem ser as polaroids e fotos enviadas?',
      answer: 'As fotos devem ser o mais naturais possível: com boa luz natural (dia), fundo neutro (parede lisa), sem maquiagem pesada, sem filtros de redes sociais, sem óculos escuros e sem bonés ou acessórios cobrindo o rosto. Recomenda-se roupas básicas de tons neutros.',
      displayOrder: 1,
      isActive: true
    },
    {
      id: '00000000-0000-0000-0000-000000000003',
      question: 'Menores de 18 anos podem se cadastrar?',
      answer: 'Sim, a WB Agency trabalha com formação e desenvolvimento de novos talentos a partir dos 13 anos. Para candidatos menores de 18 anos, é estritamente obrigatório o consentimento e preenchimento dos dados do responsável legal (nome completo, CPF, telefone e e-mail).',
      displayOrder: 2,
      isActive: true
    },
    {
      id: '00000000-0000-0000-0000-000000000004',
      question: 'Como e quando saberei o resultado da avaliação?',
      answer: 'Nossa banca de diretores de casting analisa todos os dossiês enviados. Devido ao alto volume de inscrições nacionais, entramos em contato em até 5 dias úteis caso o seu perfil atenda às demandas atuais de campanhas e clientes da agência.',
      displayOrder: 3,
      isActive: true
    }
  ];

  getPublicFaqs(): Observable<ApplyFaq[]> {
    return this.api.get<ApplyFaq[]>('/public/apply-faq').pipe(
      catchError(err => {
        console.warn('Backend FAQ inacessível, utilizando fallback editorial:', err);
        return of(this.defaultFaqs);
      })
    );
  }

  getPublicApplyHeader(): Observable<ApplyHeader> {
    return this.api.get<ApplyHeader>('/public/institutional/apply-header').pipe(
      catchError(err => {
        console.warn('Backend cabeçalho apply inacessível, utilizando fallback editorial:', err);
        return of(this.defaultHeader);
      })
    );
  }

  getAdminFaqs(): Observable<ApplyFaq[]> {
    return this.api.get<ApplyFaq[]>('/admin/apply-faq').pipe(
      catchError(err => {
        console.warn('Backend admin FAQ inacessível, utilizando fallback offline:', err);
        return of(this.defaultFaqs);
      })
    );
  }

  createFaq(dto: ApplyFaqCreateUpdate): Observable<ApplyFaq> {
    return this.api.post<ApplyFaq>('/admin/apply-faq', dto);
  }

  updateFaq(id: string, dto: ApplyFaqCreateUpdate): Observable<ApplyFaq> {
    return this.api.put<ApplyFaq>(`/admin/apply-faq/${id}`, dto);
  }

  toggleStatus(id: string): Observable<ApplyFaq> {
    return this.api.patch<ApplyFaq>(`/admin/apply-faq/${id}/status`);
  }

  reorderFaqs(dto: ApplyFaqReorder): Observable<void> {
    return this.api.patch<void>('/admin/apply-faq/reorder', dto);
  }

  deleteFaq(id: string): Observable<void> {
    return this.api.delete<void>(`/admin/apply-faq/${id}`);
  }

  getAdminApplyHeader(): Observable<ApplyHeader> {
    return this.api.get<ApplyHeader>('/admin/institutional/apply-header').pipe(
      catchError(err => {
        console.warn('Backend admin apply header inacessível, fallback:', err);
        return of(this.defaultHeader);
      })
    );
  }

  updateApplyHeader(dto: ApplyHeader): Observable<ApplyHeader> {
    return this.api.put<ApplyHeader>('/admin/institutional/apply-header', dto);
  }
}
