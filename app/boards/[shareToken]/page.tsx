import React from 'react';
import { notFound } from 'next/navigation';
import { MOCK_MODELS } from '@/lib/data/mock-models';
import { SharedBoardView } from '@/components/board/SharedBoardView';

interface PageProps {
  params: {
    shareToken: string;
  };
  searchParams?: {
    models?: string;
    title?: string;
  };
}

export function generateMetadata({ params, searchParams }: PageProps) {
  const title = searchParams?.title ? decodeURIComponent(searchParams.title) : 'Casting Board Oficial';
  return {
    title: `${title} | WB Scouting B2B`,
    description: `Apresentação exclusiva de casting e seleção de modelos WB Scouting para clientes e diretores de arte.`,
  };
}

export default function SharedBoardPage({ params, searchParams }: PageProps) {
  const { shareToken } = params;

  // Resolução de modelos selecionados:
  // Suporta passagem de IDs via query params ou seleção geral de mock para demonstração rica
  let selectedModels = MOCK_MODELS;

  if (searchParams?.models) {
    const ids = searchParams.models.split(',');
    const filtered = MOCK_MODELS.filter((m) => ids.includes(m.id));
    if (filtered.length > 0) {
      selectedModels = filtered;
    }
  }

  const title = searchParams?.title
    ? decodeURIComponent(searchParams.title)
    : 'Apresentação de Casting & Seleção B2B';

  return (
    <div className="bg-white min-h-[85vh]">
      <SharedBoardView
        shareToken={shareToken}
        title={title}
        clientName="Diretoria de Casting & Produção"
        notes="Apresentação com disponibilidade prévia confirmada. Composite oficial e opções de passarela e publicidade inclusos."
        expiresAt={new Date(Date.now() + 15 * 24 * 60 * 60 * 1000).toISOString()}
        models={selectedModels}
        isPasswordProtected={false}
      />
    </div>
  );
}
