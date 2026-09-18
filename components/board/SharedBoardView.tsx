'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { ModelWithMedia } from '@/types/casting';
import { ModelCard } from '@/components/casting/ModelCard';
import { formatWhatsAppLink } from '@/lib/utils/formatters';

interface SharedBoardViewProps {
  shareToken: string;
  title: string;
  clientName?: string | null;
  notes?: string | null;
  expiresAt?: string | null;
  models: ModelWithMedia[];
  isPasswordProtected?: boolean;
  expectedPassword?: string | null;
}

export const SharedBoardView: React.FC<SharedBoardViewProps> = ({
  shareToken,
  title,
  clientName,
  notes,
  expiresAt,
  models,
  isPasswordProtected = false,
  expectedPassword,
}) => {
  const [passwordInput, setPasswordInput] = useState('');
  const [isUnlocked, setIsUnlocked] = useState(!isPasswordProtected);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const handleUnlock = (e: React.FormEvent) => {
    e.preventDefault();
    if (!expectedPassword || passwordInput === expectedPassword) {
      setIsUnlocked(true);
      setErrorMsg(null);
    } else {
      setErrorMsg('Palavra-passe incorreta. Solicite a credencial ao booker responsável.');
    }
  };

  // Botão de Contato B2B para fechar o casting
  const bookingMessage = `Olá, WB Scouting! Estou revisando o Casting Board "${title}" (Ref: ${shareToken}) com ${models.length} modelos selecionados e gostaria de avançar no processo de contratação.`;
  const whatsappUrl = formatWhatsAppLink('5511999999999', bookingMessage);

  if (!isUnlocked) {
    return (
      <div className="min-h-[70vh] flex items-center justify-center px-4">
        <div className="max-w-md w-full p-8 border border-black bg-white shadow-xl text-center space-y-6">
          <div className="text-[10px] font-mono tracking-widest text-neutral-500 uppercase">
            WB SCOUTING | ACESSO CORPORATIVO RESTRITO
          </div>
          <h2 className="text-2xl font-bold uppercase tracking-tight text-black font-sans">
            Casting Board Protegido
          </h2>
          <p className="text-xs font-sans text-neutral-600">
            Esta apresentação foi gerada para <strong>{clientName || 'Cliente Corporativo'}</strong> e
            exige autenticação por palavra-passe.
          </p>

          <form onSubmit={handleUnlock} className="space-y-4">
            {errorMsg && (
              <div className="text-xs font-mono text-red-600 bg-red-50 p-2 border border-red-200">
                {errorMsg}
              </div>
            )}
            <input
              type="password"
              required
              placeholder="DIGITE A PALAVRA-PASSE..."
              value={passwordInput}
              onChange={(e) => setPasswordInput(e.target.value)}
              className="w-full border border-neutral-300 p-2.5 text-xs font-mono uppercase tracking-wider text-black focus:border-black focus:outline-none"
            />
            <button
              type="submit"
              className="w-full bg-black text-white py-2.5 text-xs font-mono uppercase tracking-widest hover:bg-neutral-800 transition-colors"
            >
              Desbloquear Apresentação &rarr;
            </button>
          </form>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      {/* Cabeçalho da Apresentação B2B */}
      <div className="border-b-2 border-black pb-6 mb-8 flex flex-col md:flex-row justify-between md:items-end gap-6">
        <div>
          <div className="flex flex-wrap items-center gap-2 mb-2">
            <span className="text-[10px] font-mono tracking-widest text-neutral-500 uppercase">
              B2B PRESENTATION BOARD
            </span>
            <span className="text-[10px] font-mono bg-neutral-100 px-2 py-0.5 text-neutral-600">
              REF: {shareToken.toUpperCase()}
            </span>
            {expiresAt && (
              <span className="text-[10px] font-mono bg-amber-50 text-amber-800 border border-amber-200 px-2 py-0.5">
                VÁLIDO ATÉ {new Date(expiresAt).toLocaleDateString('pt-BR')}
              </span>
            )}
          </div>

          <h1 className="text-3xl sm:text-5xl font-black uppercase tracking-tight text-black font-sans">
            {title}
          </h1>

          {clientName && (
            <div className="text-xs font-mono text-neutral-600 mt-1">
              PROJETO PARA: <strong className="text-black uppercase">{clientName}</strong>
            </div>
          )}
        </div>

        {/* Ação Comercial B2B */}
        <div className="flex flex-wrap gap-3">
          <a
            href={whatsappUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="bg-black text-white px-6 py-2.5 text-xs font-mono uppercase tracking-widest hover:bg-neutral-800 transition-colors flex items-center gap-2"
          >
            <span>Reservar Modelos Selecionados</span>
          </a>
        </div>
      </div>

      {/* Observações do Produtor */}
      {notes && (
        <div className="mb-8 p-4 bg-neutral-50 border border-neutral-200 text-xs font-mono text-neutral-700">
          <span className="block text-[10px] text-neutral-400 uppercase tracking-widest mb-1">
            NOTAS DA PRODUÇÃO / AGÊNCIA:
          </span>
          {notes}
        </div>
      )}

      {/* Grid de Modelos Selecionados */}
      <div className="mb-12">
        <div className="flex justify-between items-center mb-6">
          <h3 className="text-sm font-mono uppercase tracking-widest text-black font-bold">
            Modelos em Consideração [{models.length}]
          </h3>
          <Link
            href="/casting"
            className="text-xs font-mono text-neutral-500 underline hover:text-black uppercase"
          >
            Explorar Outras Opções de Casting &rarr;
          </Link>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
          {models.map((model, idx) => (
            <ModelCard key={model.id} model={model} priority={idx < 4} />
          ))}
        </div>
      </div>

      {/* Rodapé da Apresentação B2B */}
      <div className="border-t border-neutral-200 pt-8 flex flex-col sm:flex-row justify-between items-center text-xs font-mono text-neutral-500 gap-4">
        <div>
          WB SCOUTING AGENCY &bull; DEPARTAMENTO COMERCIAL & BOOKING &bull; +55 (11) 99999-9999
        </div>
        <div className="text-[10px]">
          POWERED BY RVIVAC GUILD &bull; CASTING INTELLIGENCE PLATFORM
        </div>
      </div>
    </div>
  );
};
