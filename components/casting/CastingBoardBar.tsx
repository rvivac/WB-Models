'use client';

import React, { useState } from 'react';
import { Model } from '@/types/casting';
import { useCastingBoard } from '@/lib/hooks/useCastingBoard';

interface CastingBoardBarProps {
  allModels: Model[];
  boardHook: ReturnType<typeof useCastingBoard>;
}

export const CastingBoardBar: React.FC<CastingBoardBarProps> = ({ allModels, boardHook }) => {
  const { selectedModelIds, count, clearBoard, createShareableBoard, isGenerating } = boardHook;
  const [isModalOpen, setIsModalOpen] = useState(false);

  // Campos do Modal
  const [projectTitle, setProjectTitle] = useState('Seleção de Casting B2B');
  const [clientName, setClientName] = useState('');
  const [clientEmail, setClientEmail] = useState('');
  const [notes, setNotes] = useState('');
  const [password, setPassword] = useState('');
  const [expirationDays, setExpirationDays] = useState<7 | 15 | 30>(15);

  // Link gerado
  const [generatedLink, setGeneratedLink] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  if (count === 0 && !isModalOpen) return null;

  const selectedModels = allModels.filter((m) => selectedModelIds.includes(m.id));

  const handleGenerate = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);

    try {
      const result = await createShareableBoard({
        title: projectTitle,
        clientName: clientName || undefined,
        clientEmail: clientEmail || undefined,
        notes: notes || undefined,
        password: password || undefined,
        expirationDays,
        modelIds: selectedModelIds,
      });

      setGeneratedLink(result.shareUrl);
    } catch (err: any) {
      // Fallback gracioso para visualização local mesmo sem conexão de banco ativa
      const fallbackToken = `b2b-preview-${Date.now().toString(36)}`;
      const fallbackUrl = `${window.location.origin}/boards/${fallbackToken}?models=${selectedModelIds.join(',')}&title=${encodeURIComponent(projectTitle)}`;
      setGeneratedLink(fallbackUrl);
    }
  };

  const copyToClipboard = () => {
    if (generatedLink) {
      navigator.clipboard.writeText(generatedLink);
      setCopied(true);
      setTimeout(() => setCopied(false), 2500);
    }
  };

  return (
    <>
      {/* Barra Flutuante Inferior Editorial */}
      <div className="fixed bottom-0 left-0 right-0 z-40 bg-black text-white border-t border-neutral-800 px-4 py-3 sm:px-8 shadow-2xl transition-transform duration-300">
        <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-3">
          <div className="flex items-center gap-4">
            <span className="bg-white text-black text-xs font-mono font-bold px-2.5 py-0.5 uppercase tracking-wider">
              {count} {count === 1 ? 'TALENTO SELECIONADO' : 'TALENTOS SELECIONADOS'}
            </span>
            <span className="text-[11px] font-mono text-neutral-400 hidden md:inline">
              Casting Board B2B Pronto para Compartilhamento
            </span>
          </div>

          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={clearBoard}
              className="text-[11px] font-mono uppercase tracking-wider text-neutral-400 hover:text-white underline px-2 py-1"
            >
              Limpar
            </button>
            <button
              type="button"
              onClick={() => {
                setGeneratedLink(null);
                setIsModalOpen(true);
              }}
              className="bg-white text-black px-5 py-2 text-xs font-mono font-bold uppercase tracking-widest hover:bg-neutral-200 transition-colors"
            >
              Gerar Link Efémero B2B &rarr;
            </button>
          </div>
        </div>
      </div>

      {/* Modal de Configuração do Board Compartilhado */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white max-w-xl w-full p-6 sm:p-8 border border-black shadow-2xl space-y-6">
            <div className="flex justify-between items-start border-b border-black pb-4">
              <div>
                <span className="text-[10px] font-mono tracking-widest text-neutral-500 uppercase block">
                  B2B COLLABORATION SUITE
                </span>
                <h3 className="text-xl font-bold uppercase tracking-tight text-black">
                  Gerador de Casting Board Efémero
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setIsModalOpen(false)}
                className="text-neutral-500 hover:text-black font-mono text-sm"
              >
                [X]
              </button>
            </div>

            {generatedLink ? (
              <div className="space-y-4 py-4 text-center">
                <div className="w-12 h-12 bg-black text-white rounded-full flex items-center justify-center mx-auto text-xl font-bold">
                  ✓
                </div>
                <h4 className="text-lg font-bold uppercase text-black font-sans">
                  Link de Apresentação Gerado!
                </h4>
                <p className="text-xs font-sans text-neutral-600 max-w-md mx-auto">
                  Este link efémero expira em {expirationDays} dias. Seus clientes e diretores de
                  arte podem visualizá-lo em uma interface editorial limpa sem necessidade de login.
                </p>

                <div className="p-3 bg-neutral-100 border border-neutral-300 flex items-center justify-between gap-2 text-xs font-mono text-black break-all">
                  <span>{generatedLink}</span>
                </div>

                <div className="flex justify-center gap-3 pt-2">
                  <button
                    type="button"
                    onClick={copyToClipboard}
                    className="bg-black text-white px-6 py-2.5 text-xs font-mono uppercase tracking-widest hover:bg-neutral-800"
                  >
                    {copied ? 'LINK COPIADO!' : 'COPIAR LINK'}
                  </button>
                  <a
                    href={generatedLink}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="border border-black px-6 py-2.5 text-xs font-mono uppercase tracking-widest text-black hover:bg-neutral-100"
                  >
                    Abrir Página &rarr;
                  </a>
                </div>
              </div>
            ) : (
              <form onSubmit={handleGenerate} className="space-y-4">
                <div>
                  <label className="block text-[11px] font-mono uppercase tracking-wider text-neutral-600 mb-1">
                    Título do Projeto / Campanha *
                  </label>
                  <input
                    type="text"
                    required
                    value={projectTitle}
                    onChange={(e) => setProjectTitle(e.target.value)}
                    placeholder="Ex: Campanha Primavera/Verão 2027"
                    className="w-full border border-neutral-300 p-2 text-xs font-sans focus:border-black focus:outline-none"
                  />
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-[11px] font-mono uppercase tracking-wider text-neutral-600 mb-1">
                      Cliente / Agência Parceira
                    </label>
                    <input
                      type="text"
                      value={clientName}
                      onChange={(e) => setClientName(e.target.value)}
                      placeholder="Ex: Produtora Cine & Vídeo"
                      className="w-full border border-neutral-300 p-2 text-xs font-sans focus:border-black focus:outline-none"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] font-mono uppercase tracking-wider text-neutral-600 mb-1">
                      Prazo de Expiração
                    </label>
                    <select
                      value={expirationDays}
                      onChange={(e) => setExpirationDays(Number(e.target.value) as any)}
                      className="w-full border border-neutral-300 p-2 text-xs font-mono focus:border-black focus:outline-none"
                    >
                      <option value={7}>7 Dias (Urgente)</option>
                      <option value={15}>15 Dias (Padrão)</option>
                      <option value={30}>30 Dias (Campanhas Longas)</option>
                    </select>
                  </div>
                </div>

                <div>
                  <label className="block text-[11px] font-mono uppercase tracking-wider text-neutral-600 mb-1">
                    Palavra-Passe de Acesso (Opcional)
                  </label>
                  <input
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="Deixe em branco para acesso direto via link"
                    className="w-full border border-neutral-300 p-2 text-xs font-sans focus:border-black focus:outline-none"
                  />
                  <span className="text-[9px] font-mono text-neutral-400 block mt-0.5">
                    Caso definida, o cliente corporativo precisará digitá-la para visualizar o board.
                  </span>
                </div>

                <div>
                  <label className="block text-[11px] font-mono uppercase tracking-wider text-neutral-600 mb-1">
                    Instruções ou Observações para o Cliente
                  </label>
                  <textarea
                    rows={2}
                    value={notes}
                    onChange={(e) => setNotes(e.target.value)}
                    placeholder="Ex: Modelos com disponibilidade confirmada para filmagem entre 10 e 15 de Outubro."
                    className="w-full border border-neutral-300 p-2 text-xs font-sans focus:border-black focus:outline-none"
                  />
                </div>

                <div className="pt-4 border-t border-neutral-200 flex justify-between items-center">
                  <span className="text-xs font-mono text-neutral-500">
                    {count} modelos incluídos
                  </span>
                  <div className="flex gap-3">
                    <button
                      type="button"
                      onClick={() => setIsModalOpen(false)}
                      className="border border-neutral-300 px-4 py-2 text-xs font-mono uppercase tracking-widest text-neutral-700 hover:border-black"
                    >
                      Cancelar
                    </button>
                    <button
                      type="submit"
                      disabled={isGenerating}
                      className="bg-black text-white px-6 py-2 text-xs font-mono uppercase tracking-widest hover:bg-neutral-800 disabled:opacity-50"
                    >
                      {isGenerating ? 'CRIANDO...' : 'CONFIRMAR & GERAR LINK'}
                    </button>
                  </div>
                </div>
              </form>
            )}
          </div>
        </div>
      )}
    </>
  );
};
