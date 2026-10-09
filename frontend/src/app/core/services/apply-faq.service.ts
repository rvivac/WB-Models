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
      id: 'e92ead7e-858b-4988-b921-230c32b9d9fb',
      question: 'Quais são as medidas ideais para o mercado da moda e publicidade?',
      answer: 'Não existem medidas certas ou específicas. O importante é ter personalidade marcante, atitude e querer muito ser modelo.',
      displayOrder: 1,
      isActive: true
    },
    {
      id: '8e12a91c-30f0-468d-ad27-576fce70bdfe',
      question: 'É exigida altura mínima?',
      answer: 'Dentro do mercado de moda e publicidade isso varia muito. Nós avaliamos todos os perfis, independente da altura.',
      displayOrder: 2,
      isActive: true
    },
    {
      id: '6c2cc246-b516-4102-9dd4-ab072e7125a8',
      question: 'Quero ser modelo! Como faço para realizar uma avaliação digital? Quais devem ser os meus primeiros passos?',
      answer: 'Primeiramente, preencher corretamente todos os dados e medidas na ficha abaixo, deixar o seu Instagram e TikTok abertos para visualização de todos e incluir todas as fotos. Independente da resposta, todos receberão um e-mail devolutivo. Se o feedback for positivo, iremos orientá-lo(a) para os próximos passos.',
      displayOrder: 3,
      isActive: true
    },
    {
      id: 'cc646a84-85b4-45d7-9ed9-233bb88e8c39',
      question: 'Como devem ser essas fotos? Preciso ter book profissional?',
      answer: 'Não é necessário pagar por um book profissional. Nós analisamos, de preferência, fotos caseiras feitas em celular. Evitar: selfies, de costas, fotos com óculos de sol ou outros acessórios.\n\nPara fazer boas fotos, encontre uma parede de cor neutra, de dia com luz natural, sem maquiagem, sem edição e sem filtros – queremos ver a sua beleza natural. Use camiseta branca ou preta básica/sem estampas e calça jeans (meninas de salto).\n\nCertifique-se de fazer fotos:\n• Do corpo inteiro e meio corpo;\n• De perfil;\n• E close do rosto – sorrindo e sem sorrir.',
      displayOrder: 4,
      isActive: true
    },
    {
      id: '27bd1b92-0927-4a7b-b353-db4ec825d29e',
      question: 'A partir de qual idade posso trabalhar na profissão de modelo?',
      answer: 'Antigamente era comum ver modelos que começavam a carreira aos 13, 14 anos, mas o mercado a cada ano se profissionaliza mais, por isso não agenciamos crianças.\n\nO mercado de moda nacional, de acordo com a legislação trabalhista, exige que os modelos profissionais tenham no mínimo 16 anos para trabalhar em qualquer tipo de campanha publicitária, fotos, filmes, desfiles… No Brasil, exige-se uma emancipação para quem tem entre 16 e 17 anos de acordo com a legislação da Justiça do Trabalho.',
      displayOrder: 5,
      isActive: true
    },
    {
      id: '1ebaf156-0203-4edb-930c-07628496e092',
      question: 'Preciso pagar alguma taxa para fazer avaliação na agência?',
      answer: 'Não, basta você enviar seus dados, medidas e fotos e aguardar o feedback da avaliação digital.',
      displayOrder: 6,
      isActive: true
    },
    {
      id: '51a45938-7450-4f6d-bf0c-6cda87f9f0b0',
      question: 'Preciso morar em SP?',
      answer: 'Os principais clientes do mercado estão localizados em SP – além de a maior semana de moda da América Latina ser realizada na cidade. Por isso, estar mais próxima desse mercado é um fator que facilita.',
      displayOrder: 7,
      isActive: true
    },
    {
      id: '00000000-0000-0000-0000-000000000001',
      question: 'Existe algum custo para inscrição ou avaliação?',
      answer: 'Não. A WB Agency nunca cobra nenhuma taxa para inscrição, avaliação de perfil, teste de vídeo ou agenciamento inicial. O processo de scouting é 100% gratuito e desconfie de qualquer abordagem cobrando taxas em nosso nome.',
      displayOrder: 8,
      isActive: false
    }
  ];

  getPublicFaqs(lang?: string): Observable<ApplyFaq[]> {
    const endpoint = lang ? `/public/apply-faq?lang=${encodeURIComponent(lang)}` : '/public/apply-faq';
    return this.api.get<ApplyFaq[]>(endpoint).pipe(
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
