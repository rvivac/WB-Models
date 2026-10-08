import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CdkDragDrop, DragDropModule, moveItemInArray } from '@angular/cdk/drag-drop';
import { ApiService } from '../../../core/services/api.service';

export interface FeaturedModelSummary {
  id: string;
  artisticName: string;
  category: string;
  height: number;
  isStar: boolean;
  coverPhotoUrl: string;
  displayOrder?: number;
}

/**
 * Extracao TOLERANTE de array de itens a partir de envelopes variados.
 * Suporta: Array direto / ApiResponse{data} / Page{content} / res.body / res.payload etc.
 */
function extractArray(res: any, fallback: any[] = []): any[] {
  if (!res) return fallback;
  if (Array.isArray(res)) return res;
  const candidates = [res, res?.data, res?.body, res?.payload, res?.result].filter(c => c != null);
  for (const c of candidates) {
    if (Array.isArray(c)) return c;
    if (Array.isArray(c.content)) return c.content;
    if (Array.isArray(c.items)) return c.items;
    if (Array.isArray(c.records)) return c.records;
  }
  if (Array.isArray(res.content)) return res.content;
  return fallback;
}

@Component({
  selector: 'app-featured-models-manager',
  standalone: true,
  imports: [CommonModule, FormsModule, DragDropModule],
  templateUrl: './featured-models-manager.component.html',
  styleUrls: ['./featured-models-manager.component.scss']
})
export class FeaturedModelsManagerComponent implements OnInit {
  private readonly api = inject(ApiService);

  // ============================================================
  // 🔥 VARIÁVEIS QUE O HTML REFERENCIA (faltavam no build)
  // ============================================================
  featuredList: FeaturedModelSummary[] = [];
  availableModels: FeaturedModelSummary[] = [];
  searchQuery = '';
  isSubmitting = false;
  /** Carregando destaques atuais? */
  loadingFeatured: boolean = true;
  /** Carregando pool de modelos disponiveis? */
  loadingAvailable: boolean = false;
  /** Mensagem de erro exibida no topo (HTML referencia diretamente) */
  lastErrorMessage: string | null = null;

  // ============================================================
  // 🔥 GETTERS QUE O HTML REFERENCIA (faltavam no build)
  // ============================================================
  /** TRUE = destaques reais estão VAZIOS após término do carregamento (não exibe mocks) */
  get isFeaturedEmptyReal(): boolean {
    return !this.loadingFeatured && (!this.featuredList || this.featuredList.length === 0);
  }
  /** TRUE = pool de modelos disponíveis está VAZIO real após término do carregamento */
  get isAvailableEmptyReal(): boolean {
    return !this.loadingAvailable && (!this.availableModels || this.availableModels.length === 0);
  }

  get filteredAvailableModels(): FeaturedModelSummary[] {
    const featuredIds = new Set(this.featuredList.map(m => m.id));
    const query = (this.searchQuery || '').trim().toLowerCase();
    return this.availableModels
      .filter(m => !featuredIds.has(m.id))
      .filter(m => !query || m.artisticName.toLowerCase().includes(query));
  }

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    console.log('[admin/destaques-home] Iniciando loadData() via ApiService (com JWT).');
    this.loadingFeatured = true;
    this.loadingAvailable = true;
    this.lastErrorMessage = null;

    // ============================================================
    // 1. Busca os destaques ATUAIS (isFeaturedHome=true) ordenados.
    //    ⛔ NÃO HÁ MAIS FALLBACK MOCK: vazio? → estado real no HTML.
    // ============================================================
    this.api.get<any[]>('/admin/featured-models').subscribe({
      next: (res) => {
        this.loadingFeatured = false;
        console.log('[admin/destaques-home] Resposta CRUA GET /admin/featured-models:', res);
        const list = extractArray(res, []);
        console.log(`[admin/destaques-home] Destaques extraídos: ${list.length} itens.`);
        if (list.length === 0) {
          console.warn('[admin/destaques-home] ⚠️  API retornou 0 destaques. Renderizando estado VAZIO real (sem mocks).');
        }
        this.featuredList = list.map((m) => this.mapModelSummary(m));
      },
      error: (err) => {
        this.loadingFeatured = false;
        this.lastErrorMessage = 'Erro ao carregar destaques da vitrine. Verifique seu login ou tente novamente.';
        console.error('[admin/destaques-home] ERRO ao buscar /admin/featured-models:', err);
        // ⛔ NÃO CHAMA MAIS loadMockFallback() — UI mostra mensagem "Nenhum destaque".
        this.featuredList = [];
      }
    });

    // ============================================================
    // 2. Busca o POOL de modelos ATIVOS (todos) para drag-and-drop.
    //    ⛔ NÃO HÁ MAIS FALLBACK MOCK: vazio? → mensagem no HTML.
    // ============================================================
    this.api.get<any>('/admin/models', { status: 'ACTIVE', size: 100, page: 0 }).subscribe({
      next: (res) => {
        this.loadingAvailable = false;
        console.log('[admin/destaques-home] Resposta CRUA GET /admin/models?status=ACTIVE:', res);
        const list = extractArray(res, []);
        console.log(`[admin/destaques-home] Pool de modelos ativos extraído: ${list.length} itens.`);
        if (list.length === 0) {
          console.warn('[admin/destaques-home] ⚠️  0 modelos ativos no casting. Admin deve cadastrar modelos primeiro.');
        }
        this.availableModels = list.map((m) => this.mapModelSummary(m));
      },
      error: (err) => {
        this.loadingAvailable = false;
        this.lastErrorMessage = 'Erro ao carregar pool de modelos ativos. Verifique seu login ou tente novamente.';
        console.error('[admin/destaques-home] ERRO ao buscar /admin/models?status=ACTIVE:', err);
        // ⛔ NÃO CHAMA MAIS loadAvailableMockFallback() — UI mostra pool vazio real.
        this.availableModels = [];
      }
    });
  }

  onDrop(event: CdkDragDrop<FeaturedModelSummary[]>): void {
    moveItemInArray(this.featuredList, event.previousIndex, event.currentIndex);
  }

  addToFeatured(model: FeaturedModelSummary): void {
    if (!this.featuredList.some(m => m.id === model.id)) {
      this.featuredList.push({
        ...model,
        displayOrder: this.featuredList.length + 1
      });
    }
  }

  removeFromFeatured(id: string): void {
    this.featuredList = this.featuredList.filter(m => m.id !== id);
  }

  saveChanges(): void {
    this.isSubmitting = true;
    this.lastErrorMessage = null;
    const payload = {
      items: this.featuredList.map((model, idx) => ({
        modelId: model.id,
        displayOrder: idx + 1
      }))
    };

    console.log('[admin/destaques-home] PUT /admin/featured-models → payload enviado:', payload);

    this.api.put<any[]>('/admin/featured-models', payload).subscribe({
      next: (savedRes) => {
        this.isSubmitting = false;
        const savedList = extractArray(savedRes, []);
        console.log(`[admin/destaques-home] PUT OK! ${savedList.length} destaques salvos no backend.`, savedRes);
        // Recarrega a lista com o que o backend CONFIRMOU (garante consistência com featuredOrder real)
        this.featuredList = savedList.map((m) => this.mapModelSummary(m));
        this.lastErrorMessage = null;
        alert('✅ Curadoria da Home atualizada com sucesso! Alterações refletidas imediatamente no banco.');
      },
      error: (err) => {
        this.isSubmitting = false;
        const detail = err?.error?.message || err?.message || String(err?.status || '');
        this.lastErrorMessage = `Erro ao publicar vitrine: ${detail}. Verifique login e tente novamente.`;
        console.error('[admin/destaques-home] ERRO PUT /admin/featured-models:', err);
        alert(`❌ Erro ao salvar nova vitrine da Home:\n${detail}\n\nVerifique se você está logado como ADM e tente novamente.`);
      }
    });
  }

  private mapModelSummary(m: any): FeaturedModelSummary {
    // Fallback de placeholder: nenhuma foto (modelo novo) não quebra layout card.
    const cover = m?.coverPhotoUrl || m?.primaryPhotoUrl || m?.bookPhotos?.find?.((p: any) => p?.isCover)?.fileUrl || m?.bookPhotos?.[0]?.fileUrl || '';
    return {
      id: m?.id ?? '',
      artisticName: (m?.artisticName || m?.stageName || 'Sem Nome').toString(),
      category: (m?.category || ((m?.gender === 'MALE' || m?.gender === 'male') ? 'COMMERCIAL' : 'FASHION')).toString(),
      height: Number(m?.height ?? m?.heightCm ?? 175),
      isStar: Boolean(m?.isStar),
      coverPhotoUrl: cover,
      displayOrder: (typeof m?.displayOrder === 'number') ? m.displayOrder : (typeof m?.featuredOrder === 'number' ? m.featuredOrder : undefined)
    };
  }
}
