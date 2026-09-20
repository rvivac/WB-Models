import { Pipe, PipeTransform } from '@angular/core';

/**
 * Normaliza um handle ou URL do Instagram para a URL canônica:
 * https://www.instagram.com/{handle}/
 * 
 * Suporta formatos:
 * - @nomedomodelo -> https://www.instagram.com/nomedomodelo/
 * - nomedomodelo -> https://www.instagram.com/nomedomodelo/
 * - https://instagram.com/nomedomodelo -> https://www.instagram.com/nomedomodelo/
 * - https://www.instagram.com/nomedomodelo/ -> https://www.instagram.com/nomedomodelo/
 * - http://instagram.com/nomedomodelo?hl=pt -> https://www.instagram.com/nomedomodelo/
 */
export function sanitizeInstagramUrl(raw: string | null | undefined): string {
  if (!raw || !raw.trim()) {
    return '';
  }

  const trimmed = raw.trim();

  // Caso seja URL completa (http/https)
  if (trimmed.startsWith('http://') || trimmed.startsWith('https://')) {
    try {
      const url = new URL(trimmed);
      // Remove barras iniciais e finais do pathname
      const segments = url.pathname.split('/').filter(Boolean);
      if (segments.length > 0) {
        const handle = segments[0].replace(/^@+/, '').trim();
        if (handle) {
          return `https://www.instagram.com/${handle}/`;
        }
      }
    } catch {
      // Fallback em caso de URL mal formatada
      const clean = trimmed
        .replace(/^https?:\/\/(www\.)?instagram\.com\/?/i, '')
        .split(/[/?#]/)[0]
        .replace(/^@+/, '')
        .trim();
      if (clean) {
        return `https://www.instagram.com/${clean}/`;
      }
    }
  }

  // Caso seja informado apenas como path (ex: instagram.com/usuario ou www.instagram.com/usuario)
  if (/^(www\.)?instagram\.com\//i.test(trimmed)) {
    const clean = trimmed
      .replace(/^(www\.)?instagram\.com\/?/i, '')
      .split(/[/?#]/)[0]
      .replace(/^@+/, '')
      .trim();
    if (clean) {
      return `https://www.instagram.com/${clean}/`;
    }
  }

  // Caso seja @handle ou apenas handle
  const cleanHandle = trimmed.replace(/^@+/, '').split(/[/?#]/)[0].trim();
  if (cleanHandle) {
    return `https://www.instagram.com/${cleanHandle}/`;
  }

  return '';
}

/**
 * Extrai o handle formatado com '@' para exibição visual limpa
 * Ex: 'https://instagram.com/nomedomodelo' -> '@nomedomodelo'
 */
export function extractInstagramHandle(raw: string | null | undefined): string {
  if (!raw || !raw.trim()) {
    return '';
  }

  const canonical = sanitizeInstagramUrl(raw);
  if (!canonical) {
    return '';
  }

  const parts = canonical.replace(/\/+$/, '').split('/');
  const handle = parts[parts.length - 1];
  return handle ? `@${handle}` : '';
}

@Pipe({
  name: 'instagramUrl',
  standalone: true,
  pure: true
})
export class InstagramUrlPipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    return sanitizeInstagramUrl(value);
  }
}
