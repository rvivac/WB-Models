import React from 'react';
import Link from 'next/link';
import { HeroStream } from '@/components/home/HeroStream';
import { ModelCard } from '@/components/casting/ModelCard';
import { MOCK_MODELS } from '@/lib/data/mock-models';

export default function HomePage() {
  // Modelos Stars em destaque
  const starModels = MOCK_MODELS.filter((m) => m.is_star);

  return (
    <div>
      {/* 1. Hero Section com Suporte a Streaming */}
      <HeroStream
        fallbackImageUrl="https://images.unsplash.com/photo-1490481651871-ab68de25d43d?auto=format&fit=crop&w=2000&q=85"
        title="WB Scouting"
        subtitle="Agência de Modelos & Gestão de Talentos"
      />

      {/* 2. Manifesto Editorial */}
      <section className="py-20 border-b border-neutral-200 bg-white">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 text-center">
          <span className="text-[10px] font-mono tracking-[0.3em] uppercase text-neutral-500 block mb-3">
            MANIFESTO DE CASTING
          </span>
          <h2 className="text-2xl sm:text-4xl font-extrabold uppercase tracking-tight text-black mb-6 leading-tight">
            Excelência editorial, diversidade de biótipos e conexão direta com as maiores marcas globais.
          </h2>
          <p className="text-sm font-sans text-neutral-600 leading-relaxed max-w-2xl mx-auto">
            A WB Scouting atua na intersecção entre a moda conceitual e a publicidade de alto impacto.
            Nossa plataforma inteligente de casting permite que produtores, estilistas e diretores de
            arte filtrem perfis com precisão biométrica milimétrica e baixem composites oficiais
            instantaneamente.
          </p>
        </div>
      </section>

      {/* 3. Vitrine Stars (Destaques da Agência) */}
      <section className="py-16 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-end border-b border-black pb-4 mb-8">
          <div>
            <span className="text-[10px] font-mono tracking-widest text-neutral-500 uppercase block">
              SELEÇÃO EXCLUSIVA
            </span>
            <h3 className="text-2xl font-bold uppercase tracking-tight text-black">
              Stars em Destaque
            </h3>
          </div>
          <Link
            href="/casting"
            className="text-xs font-mono uppercase tracking-widest text-black underline hover:text-neutral-600"
          >
            Ver Todo o Casting &rarr;
          </Link>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
          {starModels.map((model, idx) => (
            <ModelCard key={model.id} model={model} priority={idx === 0} />
          ))}
        </div>
      </section>

      {/* 4. Banner de Chamada para Scouting ("Quero ser modelo") */}
      <section className="bg-neutral-950 text-white py-20 my-12 border-t border-b border-neutral-800">
        <div className="max-w-5xl mx-auto px-4 sm:px-6 text-center space-y-6">
          <span className="text-[11px] font-mono tracking-[0.3em] uppercase text-neutral-400">
            TALENT RECRUITMENT
          </span>
          <h2 className="text-3xl sm:text-5xl font-black uppercase tracking-tight">
            Quer fazer parte do nosso casting?
          </h2>
          <p className="text-sm text-neutral-400 max-w-xl mx-auto leading-relaxed">
            Se você sonha com uma carreira nacional ou internacional na moda, envie suas fotos e
            medidas através do nosso funil de scouting. Nossa avaliação é rigorosa, segura e em
            conformidade com a LGPD.
          </p>
          <div className="pt-4">
            <Link
              href="/scouting"
              className="inline-block bg-white text-black px-10 py-3.5 text-xs font-mono uppercase tracking-widest hover:bg-neutral-200 transition-colors"
            >
              Iniciar Inscrição no Scouting
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}
