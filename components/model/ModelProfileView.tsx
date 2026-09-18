'use client';

import React, { useState } from 'react';
import Image from 'next/image';
import { ModelWithMedia } from '@/types/casting';
import { ModelComposite } from '@/components/casting/ModelComposite';
import {
  formatHeight,
  formatMeasurements,
  formatWhatsAppLink,
  EYE_COLOR_LABELS,
  HAIR_COLOR_LABELS,
  GENDER_LABELS,
} from '@/lib/utils/formatters';

interface ModelProfileViewProps {
  model: ModelWithMedia;
  agencyPhone?: string;
}

export const ModelProfileView: React.FC<ModelProfileViewProps> = ({
  model,
  agencyPhone = '5511999999999',
}) => {
  const [activeTab, setActiveTab] = useState<'book' | 'polaroid' | 'composite'>('book');

  // Filtragem de fotos por categoria
  const bookPhotos = model.media.filter((m) => m.category !== 'polaroid');
  const polaroids = model.media.filter((m) => m.category === 'polaroid');

  // Gerador de mensagem comercial automática
  const commercialMessage = `Olá, WB Scouting! Gostaria de solicitar disponibilidade e orçamento de casting para o(a) modelo ${model.artistic_name} (Ref: ${model.slug}).`;
  const whatsappUrl = formatWhatsAppLink(agencyPhone, commercialMessage);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      {/* Bloco Superior: Identificação e Ações Principais */}
      <div className="border-b-2 border-black pb-6 mb-8 flex flex-col md:flex-row justify-between md:items-end gap-6">
        <div>
          <div className="flex items-center gap-3 mb-2">
            <span className="text-[11px] font-mono tracking-widest text-neutral-500 uppercase">
              {model.is_star ? 'STAR TALENT' : 'MAIN BOARD'} &bull; {GENDER_LABELS[model.gender]}
            </span>
            {model.city && (
              <span className="text-[10px] font-mono bg-neutral-100 px-2 py-0.5 text-neutral-600">
                {model.city} / {model.state}
              </span>
            )}
          </div>
          <h1 className="text-3xl sm:text-5xl font-black uppercase tracking-tight text-black font-sans">
            {model.artistic_name}
          </h1>
        </div>

        {/* Ações Comerciais e Composite */}
        <div className="flex flex-wrap gap-3">
          <button
            type="button"
            onClick={() => setActiveTab('composite')}
            className={`border px-5 py-2.5 text-xs font-mono uppercase tracking-widest transition-colors ${
              activeTab === 'composite'
                ? 'bg-black text-white border-black'
                : 'border-black text-black hover:bg-neutral-100'
            }`}
          >
            Visualizar Composite
          </button>

          <a
            href={whatsappUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="bg-emerald-700 text-white px-6 py-2.5 text-xs font-mono uppercase tracking-widest hover:bg-emerald-800 transition-colors flex items-center gap-2"
          >
            <span>Reservar / Booker</span>
          </a>
        </div>
      </div>

      {/* Grid Principal: Biometria à Esquerda + Conteúdo/Abas à Direita */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-10">
        {/* Coluna de Biometria e Ficha Técnica (4 Colunas) */}
        <aside className="lg:col-span-4 space-y-6">
          <div className="border border-neutral-200 p-6 bg-neutral-50">
            <h3 className="text-xs font-mono uppercase tracking-widest text-neutral-400 mb-4 border-b border-neutral-200 pb-2">
              Ficha Biométrica
            </h3>

            <dl className="space-y-2.5 text-xs font-mono">
              <div className="flex justify-between py-1 border-b border-neutral-200">
                <dt className="text-neutral-500">Altura</dt>
                <dd className="font-bold text-black">{formatHeight(model.height_cm)}</dd>
              </div>
              <div className="flex justify-between py-1 border-b border-neutral-200">
                <dt className="text-neutral-500">Busto / Tórax</dt>
                <dd className="font-bold text-black">{Math.round(model.bust_chest_cm)} cm</dd>
              </div>
              <div className="flex justify-between py-1 border-b border-neutral-200">
                <dt className="text-neutral-500">Cintura</dt>
                <dd className="font-bold text-black">{Math.round(model.waist_cm)} cm</dd>
              </div>
              <div className="flex justify-between py-1 border-b border-neutral-200">
                <dt className="text-neutral-500">Quadril</dt>
                <dd className="font-bold text-black">{Math.round(model.hips_cm)} cm</dd>
              </div>
              <div className="flex justify-between py-1 border-b border-neutral-200">
                <dt className="text-neutral-500">Manequim</dt>
                <dd className="font-bold text-black">{model.dress_size}</dd>
              </div>
              <div className="flex justify-between py-1 border-b border-neutral-200">
                <dt className="text-neutral-500">Calçado</dt>
                <dd className="font-bold text-black">{model.shoe_size}</dd>
              </div>
              <div className="flex justify-between py-1 border-b border-neutral-200">
                <dt className="text-neutral-500">Olhos</dt>
                <dd className="font-bold text-black">{EYE_COLOR_LABELS[model.eye_color]}</dd>
              </div>
              <div className="flex justify-between py-1 border-b border-neutral-200">
                <dt className="text-neutral-500">Cabelo</dt>
                <dd className="font-bold text-black">{HAIR_COLOR_LABELS[model.hair_color]}</dd>
              </div>
            </dl>
          </div>

          {/* Bio Editorial */}
          {model.bio_pt && (
            <div className="p-6 border border-neutral-200 bg-white">
              <h3 className="text-xs font-mono uppercase tracking-widest text-neutral-400 mb-3">
                Perfil Artístico
              </h3>
              <p className="text-xs font-sans text-neutral-700 leading-relaxed">
                {model.bio_pt}
              </p>
            </div>
          )}

          {/* Instagram Link */}
          {model.instagram_handle && (
            <div className="text-xs font-mono">
              <a
                href={`https://instagram.com/${model.instagram_handle.replace('@', '')}`}
                target="_blank"
                rel="noopener noreferrer"
                className="text-neutral-600 hover:text-black underline"
              >
                Instagram: {model.instagram_handle} &rarr;
              </a>
            </div>
          )}
        </aside>

        {/* Coluna Dinâmica de Mídias e Composite (8 Colunas) */}
        <main className="lg:col-span-8">
          {/* Navegação por Abas */}
          <div className="border-b border-neutral-200 mb-6 flex gap-6 text-xs font-mono uppercase tracking-wider">
            <button
              type="button"
              onClick={() => setActiveTab('book')}
              className={`pb-2 transition-colors ${
                activeTab === 'book'
                  ? 'border-b-2 border-black font-bold text-black'
                  : 'text-neutral-500 hover:text-black'
              }`}
            >
              Book Fotográfico [{bookPhotos.length + 1}]
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('polaroid')}
              className={`pb-2 transition-colors ${
                activeTab === 'polaroid'
                  ? 'border-b-2 border-black font-bold text-black'
                  : 'text-neutral-500 hover:text-black'
              }`}
            >
              Polaroids & Medidas [{polaroids.length}]
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('composite')}
              className={`pb-2 transition-colors ${
                activeTab === 'composite'
                  ? 'border-b-2 border-black font-bold text-black'
                  : 'text-neutral-500 hover:text-black'
              }`}
            >
              Composite Oficial
            </button>
          </div>

          {/* Conteúdo Aba: Book Fotográfico */}
          {activeTab === 'book' && (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* Foto de Capa Principal */}
              <div className="relative aspect-[3/4] bg-neutral-100 border border-neutral-200">
                <Image
                  src={model.hero_image_key}
                  alt={`Capa de ${model.artistic_name}`}
                  fill
                  priority
                  className="object-cover"
                />
              </div>
              {/* Demais Fotos do Book */}
              {bookPhotos.map((photo) => (
                <div key={photo.id} className="relative aspect-[3/4] bg-neutral-100 border border-neutral-200">
                  <Image
                    src={photo.media_url}
                    alt={photo.caption || `Book de ${model.artistic_name}`}
                    fill
                    className="object-cover"
                  />
                </div>
              ))}
            </div>
          )}

          {/* Conteúdo Aba: Polaroids */}
          {activeTab === 'polaroid' && (
            <div>
              {polaroids.length > 0 ? (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  {polaroids.map((photo) => (
                    <div key={photo.id} className="relative aspect-[3/4] bg-neutral-100 border border-neutral-200">
                      <Image
                        src={photo.media_url}
                        alt={`Polaroid de ${model.artistic_name}`}
                        fill
                        className="object-cover"
                      />
                    </div>
                  ))}
                </div>
              ) : (
                <div className="p-12 text-center border border-dashed border-neutral-200 font-mono text-xs text-neutral-400">
                  NENHUMA POLAROID DISPONÍVEL NO MOMENTO.
                </div>
              )}
            </div>
          )}

          {/* Conteúdo Aba: Composite Oficial */}
          {activeTab === 'composite' && (
            <div>
              <ModelComposite model={model} />
            </div>
          )}
        </main>
      </div>
    </div>
  );
};
