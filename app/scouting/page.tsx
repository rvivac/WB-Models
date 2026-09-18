import React from 'react';
import { ScoutingFunnel } from '@/components/scouting/ScoutingFunnel';

export const metadata = {
  title: 'Quero ser Modelo | Funil de Scouting | WB Scouting',
  description: 'Inscreva-se para a seletiva de novos talentos da agência WB Scouting. Envie suas fotos e medidas corporais com total segurança e conformidade com a LGPD.',
};

export default function ScoutingPage() {
  return (
    <div className="bg-neutral-50 min-h-[85vh] py-8">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 text-center mb-6">
        <span className="text-[10px] font-mono tracking-widest text-neutral-500 uppercase block mb-1">
          CARREIRA & GESTÃO INTERNACIONAL
        </span>
        <h1 className="text-3xl sm:text-4xl font-extrabold uppercase tracking-tight text-black">
          Seletiva de Modelos WB Scouting
        </h1>
        <p className="text-xs font-sans text-neutral-600 max-w-lg mx-auto mt-2">
          Preencha suas informações com precisão. As imagens são processadas e comprimidas diretamente no seu
          navegador para maior rapidez e segurança.
        </p>
      </div>

      <ScoutingFunnel />
    </div>
  );
}
