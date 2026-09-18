'use client';

import React, { useState } from 'react';
import Image from 'next/image';
import Link from 'next/link';

interface HeroStreamProps {
  cloudflareStreamId?: string;
  fallbackImageUrl: string;
  title?: string;
  subtitle?: string;
}

/**
 * Hero Section Editorial com Suporte a Vídeo em Streaming (Cloudflare Stream)
 * e Fallback Otimizado para Imagem Estática com Prioridade de Carregamento (LCP).
 */
export const HeroStream: React.FC<HeroStreamProps> = ({
  cloudflareStreamId,
  fallbackImageUrl,
  title = 'WB SCOUTING',
  subtitle = 'REDEFININDO O CASTING DE MODA CONTEMPORÂNEO',
}) => {
  const [isVideoLoaded, setIsVideoLoaded] = useState(false);

  return (
    <section className="relative w-full h-[85vh] min-h-[550px] bg-neutral-950 overflow-hidden flex items-center justify-center">
      {/* Imagem de Capa Otimizada (Sempre presente como Fallback & Prioridade LCP) */}
      <div className="absolute inset-0 z-0">
        <Image
          src={fallbackImageUrl}
          alt="WB Scouting Editorial Hero"
          fill
          priority
          sizes="100vw"
          className={`object-cover object-center transition-opacity duration-1000 ${
            isVideoLoaded ? 'opacity-0' : 'opacity-80'
          }`}
        />
      </div>

      {/* Camada de Vídeo Streaming Cloudflare (se fornecido) */}
      {cloudflareStreamId && (
        <div className="absolute inset-0 z-0 pointer-events-none">
          <iframe
            src={`https://customer-${process.env.NEXT_PUBLIC_CLOUDFLARE_ACCOUNT_ID || 'demo'}.cloudflarestream.com/${cloudflareStreamId}/iframe?muted=true&autoplay=true&loop=true&controls=false`}
            className="w-full h-full object-cover border-0"
            allow="autoplay; fullscreen"
            onLoad={() => setIsVideoLoaded(true)}
            title="WB Scouting Fashion Reel"
          />
        </div>
      )}

      {/* Overlay Escuro com Gradiente Editorial */}
      <div className="absolute inset-0 z-10 bg-gradient-to-t from-black/80 via-black/40 to-black/30" />

      {/* Conteúdo Tipográfico Editorial */}
      <div className="relative z-20 max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 text-center text-white">
        <div className="text-xs sm:text-sm font-mono tracking-[0.3em] uppercase text-neutral-300 mb-3">
          {subtitle}
        </div>
        <h1 className="text-4xl sm:text-6xl md:text-7xl font-black uppercase tracking-tighter mb-6 font-sans">
          {title}
        </h1>
        <div className="flex flex-wrap justify-center gap-4 mt-8">
          <Link
            href="/casting"
            className="bg-white text-black px-8 py-3 text-xs font-mono uppercase tracking-widest hover:bg-neutral-200 transition-colors"
          >
            Explorar Casting
          </Link>
          <Link
            href="/scouting"
            className="border border-white text-white px-8 py-3 text-xs font-mono uppercase tracking-widest hover:bg-white hover:text-black transition-colors"
          >
            Quero ser Modelo
          </Link>
        </div>
      </div>

      {/* Indicador Técnico de Scroll */}
      <div className="absolute bottom-6 z-20 left-1/2 -translate-x-1/2 text-[10px] font-mono text-neutral-400 uppercase tracking-widest">
        [ DESLIZE PARA EXPLORAR ]
      </div>
    </section>
  );
};
