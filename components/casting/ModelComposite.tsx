'use client';

import React, { useRef } from 'react';
import Image from 'next/image';
import { ModelWithMedia } from '@/types/casting';
import { formatHeight, formatMeasurements, EYE_COLOR_LABELS, HAIR_COLOR_LABELS } from '@/lib/utils/formatters';

interface ModelCompositeProps {
  model: ModelWithMedia;
}

/**
 * Composite Dinâmico de Casting em Formato Editorial Canônico (Padrão A4 / Agência Internacional)
 * Permite visualização em tela e acionamento direto de impressão/exportação para PDF vetorizado.
 */
export const ModelComposite: React.FC<ModelCompositeProps> = ({ model }) => {
  const printRef = useRef<HTMLDivElement>(null);

  const handlePrint = () => {
    window.print();
  };

  // Separação inteligente de fotos: Capa/Headshot + 3 fotos de suporte
  const coverImage = model.hero_image_key;
  const secondaryPhotos = model.media.slice(0, 3);

  return (
    <div className="w-full">
      {/* Barra de Ação Superior */}
      <div className="flex justify-between items-center mb-6 no-print">
        <div>
          <span className="text-[10px] font-mono tracking-widest text-neutral-500 uppercase block">
            MATERIAL DE APRESENTAÇÃO B2B
          </span>
          <h2 className="text-xl font-bold uppercase tracking-tight text-black">
            Composite Oficial
          </h2>
        </div>
        <button
          type="button"
          onClick={handlePrint}
          className="flex items-center gap-2 border border-black bg-black text-white px-5 py-2 text-xs font-mono uppercase tracking-widest hover:bg-neutral-800 transition-colors"
        >
          <span>Baixar / Imprimir PDF</span>
        </button>
      </div>

      {/* Prancha do Composite (Formato A4 proporcional: 210mm x 297mm) */}
      <div
        ref={printRef}
        id="model-composite-sheet"
        className="mx-auto bg-white p-8 border border-neutral-300 shadow-sm print:border-none print:p-0 print:shadow-none max-w-4xl"
        style={{ minHeight: '800px' }}
      >
        {/* Cabeçalho Técnico do Composite */}
        <div className="border-b-2 border-black pb-4 mb-6 flex justify-between items-end">
          <div>
            <div className="text-[10px] font-mono tracking-widest text-neutral-500 uppercase">
              WB SCOUTING AGENCY | CASTING INTELLIGENCE
            </div>
            <h1 className="text-3xl font-black uppercase tracking-tight text-black mt-1">
              {model.artistic_name}
            </h1>
          </div>
          <div className="text-right text-[10px] font-mono text-neutral-600">
            <div>REF ID: {model.slug.toUpperCase()}</div>
            <div>STATUS: {model.is_star ? 'STAR TALENT' : 'MAIN BOARD'}</div>
          </div>
        </div>

        {/* Layout Canônico: Headshot Grande à esquerda + Grid de 3 Fotos à direita */}
        <div className="grid grid-cols-12 gap-4 mb-6">
          {/* Foto Principal (Headshot Editorial - 7 Colunas) */}
          <div className="col-span-7 relative aspect-[3/4] bg-neutral-100 border border-neutral-200 overflow-hidden">
            <Image
              src={coverImage}
              alt={`Headshot de ${model.artistic_name}`}
              fill
              className="object-cover object-top"
              priority
            />
          </div>

          {/* Fotos Complementares (3 Fotos - 5 Colunas) */}
          <div className="col-span-5 flex flex-col gap-3 justify-between">
            {secondaryPhotos.map((item, index) => (
              <div
                key={item.id || index}
                className="relative aspect-[4/3] bg-neutral-100 border border-neutral-200 overflow-hidden flex-1"
              >
                <Image
                  src={item.media_url}
                  alt={`Foto ${index + 1} de ${model.artistic_name}`}
                  fill
                  className="object-cover"
                />
              </div>
            ))}
            {/* Fallback caso faltem fotos adicionais para compor o grid */}
            {secondaryPhotos.length < 3 &&
              Array.from({ length: 3 - secondaryPhotos.length }).map((_, idx) => (
                <div
                  key={`fallback-${idx}`}
                  className="relative aspect-[4/3] bg-neutral-50 border border-neutral-200 flex items-center justify-center text-[10px] font-mono text-neutral-400"
                >
                  [EDITORIAL ARCHIVE]
                </div>
              ))}
          </div>
        </div>

        {/* Tabela de Medidas Biométricas Padronizada */}
        <div className="border-t-2 border-b-2 border-black py-3 mb-6">
          <div className="grid grid-cols-4 sm:grid-cols-8 gap-3 text-center font-mono">
            <div>
              <span className="block text-[9px] text-neutral-400 uppercase">Altura</span>
              <span className="text-xs font-bold text-black">{formatHeight(model.height_cm).split(' / ')[0]}</span>
            </div>
            <div>
              <span className="block text-[9px] text-neutral-400 uppercase">Busto/Tórax</span>
              <span className="text-xs font-bold text-black">{Math.round(model.bust_chest_cm)} cm</span>
            </div>
            <div>
              <span className="block text-[9px] text-neutral-400 uppercase">Cintura</span>
              <span className="text-xs font-bold text-black">{Math.round(model.waist_cm)} cm</span>
            </div>
            <div>
              <span className="block text-[9px] text-neutral-400 uppercase">Quadril</span>
              <span className="text-xs font-bold text-black">{Math.round(model.hips_cm)} cm</span>
            </div>
            <div>
              <span className="block text-[9px] text-neutral-400 uppercase">Manequim</span>
              <span className="text-xs font-bold text-black">{model.dress_size}</span>
            </div>
            <div>
              <span className="block text-[9px] text-neutral-400 uppercase">Calçado</span>
              <span className="text-xs font-bold text-black">{model.shoe_size}</span>
            </div>
            <div>
              <span className="block text-[9px] text-neutral-400 uppercase">Olhos</span>
              <span className="text-xs font-bold text-black">{EYE_COLOR_LABELS[model.eye_color]}</span>
            </div>
            <div>
              <span className="block text-[9px] text-neutral-400 uppercase">Cabelo</span>
              <span className="text-xs font-bold text-black">{HAIR_COLOR_LABELS[model.hair_color]}</span>
            </div>
          </div>
        </div>

        {/* Rodapé Comercial de Agência */}
        <div className="flex justify-between items-center text-[10px] font-mono text-neutral-600">
          <div>
            <strong>WB SCOUTING AGENCY</strong> | BOOKING & CASTING DIRECT<br />
            booking@wbscouting.com | +55 11 99999-9999 | www.wbscouting.com
          </div>
          <div className="text-right">
            SÃO PAULO - BRASIL<br />
            <span className="text-[8px] text-neutral-400">ENGINEERED BY RVIVAC GUILD</span>
          </div>
        </div>
      </div>

      {/* Estilos dedicados para impressão limpa (CSS Paged Media) */}
      <style jsx global>{`
        @media print {
          body {
            background-color: #fff !important;
            color: #000 !important;
          }
          .no-print {
            display: none !important;
          }
          #model-composite-sheet {
            border: none !important;
            box-shadow: none !important;
            padding: 0 !important;
            margin: 0 !important;
            width: 100% !important;
            max-width: 100% !important;
          }
          @page {
            size: A4 portrait;
            margin: 10mm;
          }
        }
      `}</style>
    </div>
  );
};
