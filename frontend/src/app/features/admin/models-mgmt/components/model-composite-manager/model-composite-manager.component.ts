import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpEventType } from '@angular/common/http';
import { environment } from '../../../../../../environments/environment';

export interface ModelComposite {
  id: string;
  fileUrl: string;
  /** Fallback se fileUrl estiver incompleta. */
  filePath?: string | null;
  /** @deprecated Use filePath. */
  storagePath?: string | null;
  fileName: string;
  fileType: 'PDF' | 'IMAGE';
  fileSizeBytes: number;
  updatedAt: string;
}

/** Placeholder SVG Base64 (Composite Card indisponivel) — tamanho fixo 56x64, WB cinza. Evita broken image icon. */
const COMPOSITE_PLACEHOLDER_DATA_URL = 'data:image/svg+xml;utf8,' + encodeURIComponent(`
<svg xmlns="http://www.w3.org/2000/svg" width="56" height="64" viewBox="0 0 56 64">
  <defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stop-color="#F4F5F4"/><stop offset="100%" stop-color="#E8EBE8"/></linearGradient></defs>
  <rect width="56" height="64" fill="url(#g)" stroke="#D1D5DB" stroke-width="1.2"/>
  <text x="50%" y="54%" font-family="ui-sans-serif,system-ui,-apple-system,Segoe UI,Roboto,sans-serif" font-size="11" font-weight="700" fill="#5F6661" text-anchor="middle">WB</text>
  <text x="50%" y="76%" font-family="ui-sans-serif,system-ui" font-size="6.5" fill="#8A918B" text-anchor="middle" letter-spacing="0.08em">COMPOSITE</text>
</svg>`);

@Component({
  selector: 'app-model-composite-manager',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './model-composite-manager.component.html',
  styleUrls: ['./model-composite-manager.component.scss']
})
export class ModelCompositeManagerComponent implements OnInit {
  @Input({ required: true }) modelId!: string;

  private readonly http = inject(HttpClient);
  composite: ModelComposite | null = null;
  /** URL FINAL segura para renderizar a previa (fileUrl corrigida via helper + bust-cache) */
  compositeSafeFileUrl: string | null = null;
  compositeBrokenPreview = signal<boolean>(false);
  isUploading = false;
  uploadProgress = 0;
  isDragging = false;
  errorMessage: string | null = null;

  private readonly allowedMimeTypes = ['application/pdf', 'image/jpeg', 'image/png', 'image/webp'];
  private readonly maxSizeBytes = 30 * 1024 * 1024; // 🟢 30 MB (alinhado com storage.validateUpload. Composite PDF InDesign = 10-25 MB normal)
  private readonly SUPABASE_PUBLIC_STORAGE_BASE = 'https://zmpqmdizqgnpnirqiufq.supabase.co/storage/v1/object/public/models-media';

  /** @returns placeholder data URL (sem rede) se a previa da imagem composite quebrar. */
  get placeholderUrl(): string { return COMPOSITE_PLACEHOLDER_DATA_URL; }

  ngOnInit(): void {
    if (this.modelId) {
      this.loadComposite();
    }
  }

  /**
   * Helper Anti-Fragil para URL composite (igual ao PublicModelService._resolveCoverForCard).
   * Remonta URL se fileUrl veio incompleta (vazia, termina com /, so /public/models-media/ sem path).
   */
  private _resolveSafeCompositeUrl(c: ModelComposite | null): string | null {
    if (!c || !c.fileUrl && !c.filePath && !c.storagePath) return null;
    const filePath = (c.filePath || c.storagePath || '').toString().trim();
    let rawUrl = (c.fileUrl || '').toString().trim();
    if (!rawUrl && filePath) {
      rawUrl = `${this.SUPABASE_PUBLIC_STORAGE_BASE}/${filePath.replace(/^\//, '')}`;
    }
    const seemsIncomplete = !rawUrl.startsWith('http')
      || rawUrl.endsWith('/')
      || (filePath && rawUrl.length < 40 && !rawUrl.includes(filePath.substring(filePath.lastIndexOf('/') + 1)));
    if (seemsIncomplete && filePath) {
      const clean = filePath.replace(/^\//, '');
      if (rawUrl && !rawUrl.startsWith('http')) {
        if (rawUrl.includes('/models-media/') && !rawUrl.includes(clean)) {
          const base = rawUrl.endsWith('/') ? rawUrl : (rawUrl + '/');
          const prefix = base.startsWith('http') ? '' : this.SUPABASE_PUBLIC_STORAGE_BASE.split('/storage/')[0];
          rawUrl = (prefix + base + clean).replace(/([^:]\/)\/+/g, '$1');
        } else if (!rawUrl.includes(clean)) {
          rawUrl = `${this.SUPABASE_PUBLIC_STORAGE_BASE}/${clean}`;
        }
      } else if (!rawUrl) {
        rawUrl = `${this.SUPABASE_PUBLIC_STORAGE_BASE}/${clean}`;
      }
    }
    if (rawUrl) {
      rawUrl = rawUrl + (rawUrl.includes('?') ? '&' : '?') + 'v=' + Math.floor(Date.now() / 3_600_000);
    }
    return rawUrl || c.fileUrl || null;
  }

  loadComposite(): void {
    this.compositeBrokenPreview.set(false);
    this.http.get<ModelComposite>(`${environment.apiUrl}/admin/models/${this.modelId}/composite`).subscribe({
      next: (data) => {
        this.composite = data;
        this.compositeSafeFileUrl = this._resolveSafeCompositeUrl(data);
        this.errorMessage = null;
      },
      error: () => {
        this.composite = null;
        this.compositeSafeFileUrl = null;
      }
    });
  }

  /** Trata evento (error) da tag img que exibe previa IMAGE do composite. Troca instantaneamente por placeholder cinza. */
  onPreviewError($event?: any): void {
    this.compositeBrokenPreview.set(true);
  }

  /** @returns URL que deve ser usada no img src da previa (ou placeholder se quebrou). */
  getCompositeImageSrc(): string {
    if (this.compositeBrokenPreview()) return this.placeholderUrl;
    if (this.compositeSafeFileUrl) return this.compositeSafeFileUrl;
    if (this.composite?.fileUrl) return this.composite.fileUrl;
    return this.placeholderUrl;
  }

  onFileSelected(event: Event): void {
    this.errorMessage = null;
    const target = event.target as HTMLInputElement;
    if (target.files && target.files.length > 0) {
      this.uploadComposite(target.files[0]);
      target.value = '';
    }
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragging = false;
    this.errorMessage = null;
    if (event.dataTransfer?.files && event.dataTransfer.files.length > 0) {
      this.uploadComposite(event.dataTransfer.files[0]);
    }
  }

  onDragOver(e: DragEvent): void {
    e.preventDefault();
    this.isDragging = true;
  }

  onDragLeave(e: DragEvent): void {
    e.preventDefault();
    this.isDragging = false;
  }

  uploadComposite(file: File): void {
    this.errorMessage = null;
    this.compositeBrokenPreview.set(false);

    // 🟢 VALIDACAO MIME BRANDA: (nao rejeita application/octet-stream / vazio se extensao eh conhecida)
    if (!this.isValidFileType(file)) {
      const ext = (file.name.toLowerCase().split('.').pop() || 'desconhecida').toUpperCase();
      this.errorMessage = `❌ Formato de arquivo inválido: ${ext}. Tipos aceitos: PDF, JPG, PNG e WEBP.`;
      return;
    }

    if (file.size > this.maxSizeBytes) {
      const realMb = (file.size / (1024 * 1024)).toFixed(1);
      this.errorMessage = `❌ Arquivo muito grande: ${realMb} MB. Limite máximo permitido é 30 MB. Compacte o PDF ou envie uma imagem menor.`;
      return;
    }

    const formData = new FormData();
    formData.append('file', file);

    this.isUploading = true;
    this.uploadProgress = 0;

    this.http.put<ModelComposite>(
      `${environment.apiUrl}/admin/models/${this.modelId}/composite`,
      formData,
      { reportProgress: true, observe: 'events' }
    ).subscribe({
      next: (event) => {
        if (event.type === HttpEventType.UploadProgress && event.total) {
          this.uploadProgress = Math.round((100 * event.loaded) / event.total);
        } else if (event.type === HttpEventType.Response) {
          const created: ModelComposite | null = (event.body as ModelComposite) || null;
          this.composite = created;
          this.compositeSafeFileUrl = created ? this._resolveSafeCompositeUrl(created) : null;
          this.compositeBrokenPreview.set(false);
          this.errorMessage = null;
          this.isUploading = false;
          this.uploadProgress = 0;
        }
      },
      error: (err: any) => {
        // 🔴 TRATAMENTO ESPECIFICO DE ERRO: LER O JSON DO BACKEND (NAO MAIS MENSAGEM GENERICA)
        this.isUploading = false;
        this.uploadProgress = 0;

        // Extrai detalhes do RFC 7807 Problem Details ou mensagem custom
        const detail = err?.error?.detail || err?.error?.message || err?.statusText || 'Erro desconhecido';
        const status: number = err?.status || 0;

        let msg = '';
        const detalheLower = String(detail).toLowerCase();

        if (status === 401) {
          msg = '🔐 Sua sessão expirou. Por favor, faça login novamente no painel Admin e reenvie o composite.';
        } else if (status === 403) {
          msg = '🚫 Permissão negada. Sua conta não tem permissão para atualizar composites.';
        } else if (status === 404) {
          msg = '⚠️ Modelo não encontrado no banco de dados. Recarregue a página (F5) e tente novamente.';
        } else if (status === 413 || detalheLower.includes('muito grande') || detalheLower.includes('limite') || detalheLower.includes('mb') || detalheLower.includes('excede')) {
          const realMb = (file.size / (1024 * 1024)).toFixed(1);
          msg = `❌ Arquivo muito grande: ${realMb} MB. Limite 30 MB. Compacte o PDF (ex: SmallPDF) e reenvie. Detalhe: ${detail}.`;
        } else if (status === 400 || detalheLower.includes('formato') || detalheLower.includes('inválido') || detalheLower.includes('invalido') || detalheLower.includes('permitidos')) {
          msg = `❌ Formato inválido. ${detail}. Aceitos: PDF, JPG, PNG, WEBP.`;
        } else if (status === 409) {
          msg = `⚠️ Conflito ao salvar composite. Tente novamente. Detalhe: ${detail}.`;
        } else if (status === 502 || status === 504 || status === 503 || detalheLower.includes('timeout') || detalheLower.includes('conexão') || detalheLower.includes('conexao') || detalheLower.includes('nuvem')) {
          msg = `🌐 Falha temporária na conexão com o armazenamento na nuvem (Render / Supabase cold-start ou alta demanda). Tente novamente em 30 segundos. Detalhe: ${detail}.`;
        } else if (status >= 500) {
          msg = `🔥 Erro interno no servidor (HTTP ${status}). A equipe WB foi notificada automaticamente. Detalhe: ${detail}.`;
        } else {
          msg = `❌ Falha ao processar o upload do Composite. Detalhe: ${detail} (Status HTTP ${status}).`;
        }

        this.errorMessage = msg;
        console.error('[Composite Manager] Erro tratado. Status:', status, 'Detail:', detail, 'Full err:', err);
      }
    });
  }

  removeComposite(): void {
    this.errorMessage = null;
    if (confirm('Tem certeza de que deseja remover o Composite oficial deste modelo?')) {
      this.http.delete(`${environment.apiUrl}/admin/models/${this.modelId}/composite`).subscribe({
        next: () => {
          this.composite = null;
          this.compositeSafeFileUrl = null;
          this.compositeBrokenPreview.set(false);
          this.errorMessage = null;
        },
        error: (err) => {
          const detail = err?.error?.detail || err?.statusText || 'Erro desconhecido';
          this.errorMessage = `❌ Erro ao excluir composite: ${detail}.`;
        }
      });
    }
  }

  formatBytes(bytes: number): string {
    if (!bytes || bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }

  private isValidFileType(file: File): boolean {
    const mime = file.type?.toLowerCase() || '';
    if (this.allowedMimeTypes.includes(mime)) {
      return true;
    }
    // 🟢 MIME BRANDO: Nao rejeita application/octet-stream ou vazio se extensao for PDF/JPG.
    // WhatsApp baixado no Windows, navegador antigo ou PDF assinado digitalmente frequentemente vem assim.
    const name = file.name.toLowerCase();
    return name.endsWith('.pdf')
      || name.endsWith('.jpg')
      || name.endsWith('.jpeg')
      || name.endsWith('.png')
      || name.endsWith('.webp')
      || name.endsWith('.heic'); // Backend vai recusar HEIC, mas a mensagem de erro sera especifica.
  }
}
