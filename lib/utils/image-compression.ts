/**
 * Validação & Compressão Client-Side de Imagens para o Scouting Engine
 * Padrão de Engenharia RVIVAC Guild: Zero dependências externas pesadas.
 * Processamento via OffscreenCanvas / HTMLCanvasElement no navegador do usuário.
 */

import { ImageValidationResult } from '@/types/casting';

export interface CompressionOptions {
  maxDimension?: number;   // Largura ou altura máxima (default 1920px)
  quality?: number;        // Fator de qualidade (0.1 a 1.0, default 0.80)
  outputFormat?: 'image/webp' | 'image/jpeg';
  maxFileSizeKb?: number;  // Teto máximo estrito de peso de arquivo (ex: 400KB)
}

/**
 * Validação estrita de corte vertical (Portrait) e dimensões mínimas antes de qualquer processamento
 * Requisito Técnico: Fotos de casting/scouting exigem orientação vertical (height > width).
 */
export async function validateImageVerticalOrientation(file: File): Promise<ImageValidationResult> {
  return new Promise((resolve) => {
    if (typeof window === 'undefined') {
      return resolve({ isValid: true });
    }

    // Validação de tipo MIME
    if (!['image/jpeg', 'image/png', 'image/webp', 'image/avif'].includes(file.type)) {
      return resolve({
        isValid: false,
        error: 'Formato inválido. Envie arquivos JPEG, PNG, WEBP ou AVIF.',
      });
    }

    const reader = new FileReader();
    reader.readAsDataURL(file);

    reader.onload = (event) => {
      const img = new Image();
      img.src = event.target?.result as string;

      img.onload = () => {
        const { width, height } = img;
        const aspectRatio = height / width;

        // 1. Verificação de Resolução Mínima
        if (width < 600 || height < 800) {
          return resolve({
            isValid: false,
            error: `Resolução insuficiente (${width}x${height}px). A imagem deve ter no mínimo 600x800px para análise dos bookers.`,
            width,
            height,
            aspectRatio,
          });
        }

        // 2. Verificação de Corte Vertical Obrigatório (Portrait)
        // Exige que a altura seja pelo menos 15% maior que a largura (proporção editorial mínima)
        if (aspectRatio < 1.15) {
          return resolve({
            isValid: false,
            error: 'Orientação incorreta. As fotos de scouting devem ser estritamente verticais (formato retrato/portrait).',
            width,
            height,
            aspectRatio,
          });
        }

        resolve({
          isValid: true,
          width,
          height,
          aspectRatio,
        });
      };

      img.onerror = () => {
        resolve({ isValid: false, error: 'Falha ao ler o cabeçalho da imagem.' });
      };
    };

    reader.onerror = () => {
      resolve({ isValid: false, error: 'Erro de leitura do arquivo no disco.' });
    };
  });
}

/**
 * Compressão client-side com reamostragem bilinear, remoção de metadados EXIF e conversão WebP
 */
export async function compressImageClientSide(
  file: File,
  options: CompressionOptions = {}
): Promise<File> {
  const {
    maxDimension = 1920,
    quality = 0.80,
    outputFormat = 'image/webp',
    maxFileSizeKb = 450,
  } = options;

  return new Promise((resolve, reject) => {
    if (typeof window === 'undefined') {
      return resolve(file);
    }

    const reader = new FileReader();
    reader.readAsDataURL(file);

    reader.onload = (event) => {
      const img = new Image();
      img.src = event.target?.result as string;

      img.onload = () => {
        let { width, height } = img;

        // Redimensionamento proporcional se exceder maxDimension
        if (width > maxDimension || height > maxDimension) {
          if (width > height) {
            height = Math.round((height * maxDimension) / width);
            width = maxDimension;
          } else {
            width = Math.round((width * maxDimension) / height);
            height = maxDimension;
          }
        }

        const canvas = document.createElement('canvas');
        canvas.width = width;
        canvas.height = height;

        const ctx = canvas.getContext('2d');
        if (!ctx) {
          return reject(new Error('Falha ao instanciar contexto gráfico Canvas.'));
        }

        // Reamostragem com suavização de alta fidelidade
        ctx.imageSmoothingEnabled = true;
        ctx.imageSmoothingQuality = 'high';
        ctx.drawImage(img, 0, 0, width, height);

        const attemptCompression = (currentQuality: number) => {
          canvas.toBlob(
            (blob) => {
              if (!blob) {
                return reject(new Error('Erro ao converter Canvas para Blob comprimido.'));
              }

              const sizeKb = Math.round(blob.size / 1024);

              // Se ainda exceder maxFileSizeKb e qualidade > 0.5, faz novo passe mais agressivo
              if (sizeKb > maxFileSizeKb && currentQuality > 0.55) {
                return attemptCompression(currentQuality - 0.15);
              }

              const ext = outputFormat === 'image/webp' ? 'webp' : 'jpg';
              const cleanBaseName = file.name
                .substring(0, file.name.lastIndexOf('.'))
                .replace(/[^a-zA-Z0-9_-]/g, '_');
              const finalFileName = `${cleanBaseName}_scout_opt.${ext}`;

              const compressedFile = new File([blob], finalFileName, {
                type: outputFormat,
                lastModified: Date.now(),
              });

              resolve(compressedFile);
            },
            outputFormat,
            currentQuality
          );
        };

        attemptCompression(quality);
      };

      img.onerror = () => reject(new Error('Erro ao renderizar imagem para otimização.'));
    };

    reader.onerror = () => reject(new Error('Erro ao acessar arquivo local.'));
  });
}
