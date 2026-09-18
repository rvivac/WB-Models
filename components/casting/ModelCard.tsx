'use client';

import React from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { Model } from '@/types/casting';
import { formatHeight } from '@/lib/utils/formatters';

interface ModelCardProps {
  model: Model;
  priority?: boolean;
  isSelected?: boolean;
  onToggleSelect?: (modelId: string) => void;
}

/**
 * Card de Modelo Editorial Minimalista com Seletor de Casting Board
 * Padrão RVIVAC Guild: Aspect-ratio 3:4 estrito, tipografia técnica,
 * alto contraste e ação de seleção com 1 clique para colaboração B2B.
 */
export const ModelCard: React.FC<ModelCardProps> = ({
  model,
  priority = false,
  isSelected = false,
  onToggleSelect,
}) => {
  return (
    <article
      className={`group relative flex flex-col bg-white border transition-all duration-300 ${
        isSelected ? 'border-black ring-2 ring-black' : 'border-neutral-200 hover:border-black'
      }`}
    >
      {/* Container de Imagem com Aspect Ratio Editorial 3:4 */}
      <div className="relative aspect-[3/4] w-full overflow-hidden bg-neutral-100">
        <Image
          src={model.hero_image_key}
          alt={`Book de ${model.artistic_name} - WB Scouting`}
          fill
          sizes="(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 25vw"
          priority={priority}
          className="object-cover object-top transition-transform duration-700 ease-out group-hover:scale-105"
        />

        {/* Badge Star Editorial */}
        {model.is_star && (
          <div className="absolute top-3 left-3 bg-black text-white px-2 py-0.5 text-[10px] font-mono tracking-widest uppercase">
            STAR
          </div>
        )}

        {/* Botão de Adição ao Casting Board B2B (Overlay Superior Direito) */}
        {onToggleSelect && (
          <button
            type="button"
            onClick={(e) => {
              e.preventDefault();
              e.stopPropagation();
              onToggleSelect(model.id);
            }}
            className={`absolute top-3 right-3 z-10 px-2 py-1 text-[10px] font-mono uppercase tracking-wider transition-all ${
              isSelected
                ? 'bg-black text-white border border-black'
                : 'bg-white/90 text-black border border-neutral-300 hover:bg-black hover:text-white backdrop-blur-xs'
            }`}
            title={isSelected ? 'Remover da seleção' : 'Adicionar ao Casting Board'}
          >
            {isSelected ? '✓ SELECIONADO' : '+ SELEÇÃO B2B'}
          </button>
        )}

        {/* Overlay sutil para hover */}
        <div className="absolute inset-0 bg-black/10 opacity-0 transition-opacity duration-300 group-hover:opacity-100 pointer-events-none" />
      </div>

      {/* Informações Técnicas e Biometria */}
      <div className="p-4 flex flex-col justify-between flex-grow">
        <div>
          <div className="flex items-baseline justify-between mb-1">
            <h3 className="text-sm font-semibold uppercase tracking-wider text-black font-sans">
              {model.artistic_name}
            </h3>
            <span className="text-[10px] font-mono text-neutral-500 uppercase">
              {model.gender === 'female' ? 'FEM' : model.gender === 'male' ? 'MASC' : 'NB'}
            </span>
          </div>

          {/* Medidas Técnicas em Linha Editorial */}
          <div className="grid grid-cols-3 gap-1 pt-2 border-t border-neutral-100 text-[11px] font-mono text-neutral-600">
            <div>
              <span className="text-[9px] text-neutral-400 block">ALTURA</span>
              {formatHeight(model.height_cm).split(' / ')[0]}
            </div>
            <div>
              <span className="text-[9px] text-neutral-400 block">MANEQUIM</span>
              {model.dress_size}
            </div>
            <div>
              <span className="text-[9px] text-neutral-400 block">CALÇADO</span>
              {model.shoe_size}
            </div>
          </div>
        </div>

        {/* Link de Ação para o Perfil e Composite */}
        <Link
          href={`/models/${model.slug}`}
          className="mt-3 block text-center border border-neutral-900 py-1.5 text-[11px] font-mono uppercase tracking-widest text-black transition-colors duration-200 hover:bg-black hover:text-white"
          aria-label={`Ver perfil completo e composite de ${model.artistic_name}`}
        >
          Ver Perfil & Composite
        </Link>
      </div>
    </article>
  );
};
