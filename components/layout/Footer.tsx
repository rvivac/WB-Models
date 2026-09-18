import React from 'react';
import Link from 'next/link';

export const Footer: React.FC = () => {
  return (
    <footer className="w-full bg-white border-t-2 border-black pt-12 pb-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8 mb-12">
          {/* Coluna 1: Marca & Manifesto */}
          <div className="md:col-span-2 space-y-3">
            <span className="text-xl font-black uppercase tracking-tight text-black font-sans">
              WB Scouting
            </span>
            <p className="text-xs font-sans text-neutral-600 max-w-md leading-relaxed">
              Agência de modelos e gestão de carreiras com atuação nacional e conexão com os
              principais mercados de moda mundiais (Milão, Paris, Nova Iorque e Londres).
              Representação autêntica, diversidade de biótipos e excelência de casting B2B.
            </p>
            <div className="text-[10px] font-mono text-neutral-400">
              SÃO PAULO / BRASIL &bull; TODOS OS DIREITOS RESERVADOS &bull; &copy; {new Date().getFullYear()}
            </div>
          </div>

          {/* Coluna 2: Navegação Rápida */}
          <div>
            <h4 className="text-xs font-mono uppercase tracking-widest text-black font-bold mb-3">
              Navegação
            </h4>
            <ul className="space-y-2 text-xs font-mono text-neutral-600">
              <li><Link href="/casting" className="hover:text-black">Casting Geral</Link></li>
              <li><Link href="/stars" className="hover:text-black">Stars WB</Link></li>
              <li><Link href="/scouting" className="hover:text-black">Quero ser Modelo</Link></li>
              <li><Link href="/agency" className="hover:text-black">A Agência</Link></li>
              <li><Link href="/contact" className="hover:text-black">Contato Comercial</Link></li>
            </ul>
          </div>

          {/* Coluna 3: Compliance, Segurança e LGPD */}
          <div>
            <h4 className="text-xs font-mono uppercase tracking-widest text-black font-bold mb-3">
              Segurança & Dados
            </h4>
            <ul className="space-y-2 text-xs font-mono text-neutral-600">
              <li><Link href="/privacy" className="hover:text-black">Política de Privacidade (LGPD)</Link></li>
              <li><Link href="/terms" className="hover:text-black">Termos de Uso de Imagem</Link></li>
              <li><span className="text-neutral-400">RLS Database Isolation: ATIVO</span></li>
              <li><span className="text-neutral-400">Anti-Scraping Shield: CLOUDFLARE</span></li>
            </ul>
          </div>
        </div>

        {/* Assinatura Técnica de Engenharia RVIVAC Guild */}
        <div className="border-t border-neutral-200 pt-6 flex flex-col sm:flex-row justify-between items-center text-[10px] font-mono text-neutral-500 gap-3">
          <div className="flex items-center gap-2">
            <span className="inline-block w-2 h-2 bg-emerald-500 rounded-full" />
            <span>WB CASTING INTELLIGENCE PLATFORM (WB-CIP) v1.0</span>
          </div>

          <div className="text-right">
            <span>ENGINEREED BY </span>
            <strong className="text-black">RVIVAC GUILD</strong>
            <span className="text-neutral-400"> | TECNOLOGIA & ALTA PERFORMANCE</span>
          </div>
        </div>
      </div>
    </footer>
  );
};
