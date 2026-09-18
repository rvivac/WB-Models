import React from 'react';
import { CastingCatalog } from '@/components/casting/CastingCatalog';
import { MOCK_MODELS } from '@/lib/data/mock-models';

export const metadata = {
  title: 'Casting & Stars | WB Scouting',
  description: 'Consulte o catálogo oficial de modelos da agência WB Scouting. Filtros biométricos em tempo real e visualização de perfis e composites.',
};

export default function CastingPage() {
  return (
    <div className="bg-white min-h-[80vh]">
      <CastingCatalog initialModels={MOCK_MODELS} />
    </div>
  );
}
