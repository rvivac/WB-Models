'use client';

import React, { useState, useMemo } from 'react';
import { Model, CastingFiltersState } from '@/types/casting';
import { EyeColor, ModelGender } from '@/types/database.types';
import { ModelCard } from './ModelCard';
import { CastingBoardBar } from './CastingBoardBar';
import { useCastingBoard } from '@/lib/hooks/useCastingBoard';
import { EYE_COLOR_LABELS } from '@/lib/utils/formatters';

interface CastingCatalogProps {
  initialModels: Model[];
}

const INITIAL_FILTERS: CastingFiltersState = {
  gender: 'all',
  isStarOnly: false,
  minHeight: 165,
  maxHeight: 195,
  dressSize: 'all',
  eyeColor: 'all',
  searchQuery: '',
};

/**
 * Catálogo de Casting WB Scouting com Filtragem em Tempo Real e Seletor de Casting Board B2B
 */
export const CastingCatalog: React.FC<CastingCatalogProps> = ({ initialModels }) => {
  const [filters, setFilters] = useState<CastingFiltersState>(INITIAL_FILTERS);
  const [isFilterPanelOpen, setIsFilterPanelOpen] = useState(false);
  const boardHook = useCastingBoard();

  // Lista única de manequins existentes para popular o select dinamicamente
  const availableDressSizes = useMemo(() => {
    const sizes = new Set(initialModels.map((m) => m.dress_size).filter(Boolean));
    return Array.from(sizes).sort();
  }, [initialModels]);

  // Filtragem em tempo real no cliente
  const filteredModels = useMemo(() => {
    return initialModels.filter((model) => {
      // 1. Filtro de Busca por Nome
      if (
        filters.searchQuery &&
        !model.artistic_name.toLowerCase().includes(filters.searchQuery.toLowerCase())
      ) {
        return false;
      }

      // 2. Filtro de Gênero
      if (filters.gender !== 'all' && model.gender !== filters.gender) {
        return false;
      }

      // 3. Filtro Stars
      if (filters.isStarOnly && !model.is_star) {
        return false;
      }

      // 4. Faixa de Altura
      if (model.height_cm < filters.minHeight || model.height_cm > filters.maxHeight) {
        return false;
      }

      // 5. Manequim
      if (filters.dressSize !== 'all' && model.dress_size !== filters.dressSize) {
        return false;
      }

      // 6. Cor dos Olhos
      if (filters.eyeColor !== 'all' && model.eye_color !== filters.eyeColor) {
        return false;
      }

      return true;
    });
  }, [initialModels, filters]);

  const handleResetFilters = () => {
    setFilters(INITIAL_FILTERS);
  };

  return (
    <div className="w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 pb-28">
      {/* Barra de Controle Editorial & Busca */}
      <div className="border-b border-black pb-6 mb-8 flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div>
          <div className="text-[11px] font-mono tracking-widest text-neutral-500 uppercase mb-1">
            WB SCOUTING | CATÁLOGO OFICIAL DE CASTING
          </div>
          <h1 className="text-2xl sm:text-3xl font-bold uppercase tracking-tight text-black">
            Casting & Stars
          </h1>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          {/* Busca Rápida */}
          <div className="relative">
            <input
              type="text"
              placeholder="BUSCAR POR NOME..."
              value={filters.searchQuery}
              onChange={(e) => setFilters((prev) => ({ ...prev, searchQuery: e.target.value }))}
              className="w-48 sm:w-64 border border-neutral-300 px-3 py-1.5 text-xs font-mono uppercase tracking-wider text-black placeholder:text-neutral-400 focus:border-black focus:outline-none"
            />
          </div>

          {/* Toggle Filtro Avançado */}
          <button
            type="button"
            onClick={() => setIsFilterPanelOpen(!isFilterPanelOpen)}
            className={`px-4 py-1.5 text-xs font-mono uppercase tracking-wider border transition-colors ${
              isFilterPanelOpen
                ? 'bg-black text-white border-black'
                : 'border-neutral-300 text-black hover:border-black'
            }`}
          >
            Filtros {isFilterPanelOpen ? '[-]' : '[+]'}
          </button>

          {/* Totalizador de Resultados */}
          <span className="text-xs font-mono text-neutral-500">
            [{filteredModels.length} {filteredModels.length === 1 ? 'MODELO' : 'MODELOS'}]
          </span>
        </div>
      </div>

      {/* Painel Expansível de Filtros Técnicos */}
      {isFilterPanelOpen && (
        <div className="bg-neutral-50 border border-neutral-200 p-6 mb-8 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-6">
          {/* Gênero */}
          <div>
            <label className="block text-[10px] font-mono uppercase tracking-widest text-neutral-500 mb-2">
              Gênero
            </label>
            <div className="flex flex-col gap-1 text-xs font-mono">
              {(['all', 'female', 'male'] as const).map((g) => (
                <label key={g} className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="radio"
                    name="gender"
                    checked={filters.gender === g}
                    onChange={() => setFilters((prev) => ({ ...prev, gender: g as ModelGender | 'all' }))}
                    className="accent-black"
                  />
                  <span>
                    {g === 'all' ? 'Todos' : g === 'female' ? 'Feminino' : 'Masculino'}
                  </span>
                </label>
              ))}
            </div>
          </div>

          {/* Exclusivo Stars */}
          <div>
            <label className="block text-[10px] font-mono uppercase tracking-widest text-neutral-500 mb-2">
              Seleção
            </label>
            <label className="flex items-center gap-2 text-xs font-mono cursor-pointer mt-1">
              <input
                type="checkbox"
                checked={filters.isStarOnly}
                onChange={(e) => setFilters((prev) => ({ ...prev, isStarOnly: e.target.checked }))}
                className="accent-black"
              />
              <span className="font-semibold text-black">Apenas Stars</span>
            </label>
          </div>

          {/* Faixa de Altura */}
          <div>
            <label className="block text-[10px] font-mono uppercase tracking-widest text-neutral-500 mb-2">
              Altura ({filters.minHeight}cm - {filters.maxHeight}cm)
            </label>
            <div className="flex flex-col gap-2">
              <input
                type="range"
                min={160}
                max={200}
                value={filters.minHeight}
                onChange={(e) =>
                  setFilters((prev) => ({ ...prev, minHeight: Number(e.target.value) }))
                }
                className="accent-black h-1 bg-neutral-200 cursor-pointer"
              />
              <input
                type="range"
                min={160}
                max={200}
                value={filters.maxHeight}
                onChange={(e) =>
                  setFilters((prev) => ({ ...prev, maxHeight: Number(e.target.value) }))
                }
                className="accent-black h-1 bg-neutral-200 cursor-pointer"
              />
            </div>
          </div>

          {/* Manequim */}
          <div>
            <label className="block text-[10px] font-mono uppercase tracking-widest text-neutral-500 mb-2">
              Manequim
            </label>
            <select
              value={filters.dressSize}
              onChange={(e) => setFilters((prev) => ({ ...prev, dressSize: e.target.value }))}
              className="w-full border border-neutral-300 bg-white px-2 py-1.5 text-xs font-mono focus:border-black focus:outline-none"
            >
              <option value="all">TODOS</option>
              {availableDressSizes.map((size) => (
                <option key={size} value={size}>
                  {size}
                </option>
              ))}
            </select>
          </div>

          {/* Cor dos Olhos & Reset */}
          <div className="flex flex-col justify-between">
            <div>
              <label className="block text-[10px] font-mono uppercase tracking-widest text-neutral-500 mb-2">
                Cor dos Olhos
              </label>
              <select
                value={filters.eyeColor}
                onChange={(e) =>
                  setFilters((prev) => ({ ...prev, eyeColor: e.target.value as EyeColor | 'all' }))
                }
                className="w-full border border-neutral-300 bg-white px-2 py-1.5 text-xs font-mono focus:border-black focus:outline-none"
              >
                <option value="all">TODAS</option>
                {Object.entries(EYE_COLOR_LABELS).map(([key, label]) => (
                  <option key={key} value={key}>
                    {label.toUpperCase()}
                  </option>
                ))}
              </select>
            </div>

            <button
              type="button"
              onClick={handleResetFilters}
              className="mt-4 text-left text-[10px] font-mono tracking-widest text-neutral-500 underline hover:text-black uppercase"
            >
              Limpar Filtros
            </button>
          </div>
        </div>
      )}

      {/* Grid Editorial Responsivo de Modelos */}
      {filteredModels.length > 0 ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6 lg:gap-8">
          {filteredModels.map((model, idx) => (
            <ModelCard
              key={model.id}
              model={model}
              priority={idx < 4}
              isSelected={boardHook.isSelected(model.id)}
              onToggleSelect={boardHook.toggleModel}
            />
          ))}
        </div>
      ) : (
        <div className="py-24 text-center border border-dashed border-neutral-300">
          <p className="text-sm font-mono text-neutral-500 uppercase tracking-widest mb-3">
            Nenhum talento encontrado com os critérios selecionados.
          </p>
          <button
            type="button"
            onClick={handleResetFilters}
            className="border border-black px-4 py-2 text-xs font-mono uppercase tracking-wider text-black hover:bg-black hover:text-white transition-colors"
          >
            Redefinir Filtros
          </button>
        </div>
      )}

      {/* Barra de Colaboração B2B (Casting Board) */}
      <CastingBoardBar allModels={initialModels} boardHook={boardHook} />
    </div>
  );
};
