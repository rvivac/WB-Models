/**
 * Compressão de Imagens Client-Side de Alta Performance
 * Padrão RVIVAC Guild: Zero dependências externas pesadas.
 * Utiliza OffscreenCanvas / HTMLCanvasElement para gerar WebP otimizado antes do envio.
 */

export interface CompressionOptions {
  maxDimension?: number; // Largura ou altura máxima (default 1920px)
  quality?: number;      // 0.1 a 1.0 (default 0.82)
  outputFormat?: 'image/webp' | 'image/jpeg';
}

export async function compressImageClientSide(
  file: File,
  options: CompressionOptions = {}
): Promise<File> {
  const {
    maxDimension = 1920,
    quality = 0.82,
    outputFormat = 'image/webp'
  } = options;

  return new Promise((resolve, reject) => {
    // Caso o browser não suporte Canvas (improvável em navegadores modernos)
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

        // Criação de canvas para re-renderização
        const canvas = document.createElement('canvas');
        canvas.width = width;
        canvas.height = height;

        const ctx = canvas.getContext('2d');
        if (!ctx) {
          return reject(new Error('Falha ao inicializar contexto 2D do Canvas.'));
        }

        // Suavização bilinear para qualidade editorial
        ctx.imageSmoothingEnabled = true;
        ctx.imageSmoothingQuality = 'high';
        ctx.drawImage(img, 0, 0, width, height);

        // Conversão para Blob e posterior File
        canvas.toBlob(
          (blob) => {
            if (!blob) {
              return reject(new Error('Erro ao converter imagem em Blob comprimido.'));
            }

            const extension = outputFormat === 'image/webp' ? 'webp' : 'jpg';
            const baseName = file.name.substring(0, file.name.lastIndexOf('.')) || file.name;
            const newFileName = `${baseName}-wb-scout.${extension}`;

            const compressedFile = new File([blob], newFileName, {
              type: outputFormat,
              lastModified: Date.now(),
            });

            resolve(compressedFile);
          },
          outputFormat,
          quality
        );
      };

      img.onerror = () => reject(new Error('Erro ao carregar a imagem para compressão.'));
    };

    reader.onerror = () => reject(new Error('Erro ao ler arquivo local.'));
  });
}
