import React from 'react';
import { notFound } from 'next/navigation';
import { MOCK_MODELS } from '@/lib/data/mock-models';
import { ModelProfileView } from '@/components/model/ModelProfileView';

interface PageProps {
  params: {
    slug: string;
  };
}

export function generateStaticParams() {
  return MOCK_MODELS.map((model) => ({
    slug: model.slug,
  }));
}

export function generateMetadata({ params }: PageProps) {
  const model = MOCK_MODELS.find((m) => m.slug === params.slug);
  if (!model) return { title: 'Modelo Não Encontrado | WB Scouting' };

  return {
    title: `${model.artistic_name} | Perfil & Composite | WB Scouting`,
    description: `Consulte medidas, fotos de book e solicite o composite oficial de ${model.artistic_name}.`,
  };
}

export default function ModelPage({ params }: PageProps) {
  const model = MOCK_MODELS.find((m) => m.slug === params.slug);

  if (!model) {
    notFound();
  }

  return (
    <div className="bg-white min-h-[85vh]">
      <ModelProfileView model={model} agencyPhone="5511999999999" />
    </div>
  );
}
