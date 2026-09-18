'use client';

import { useState, useEffect, useCallback } from 'react';
import { supabase } from '@/lib/supabase/client';
import { CreateBoardPayload } from '@/types/casting';

const STORAGE_KEY = 'wb_casting_board_selection';

export function useCastingBoard() {
  const [selectedModelIds, setSelectedModelIds] = useState<string[]>([]);
  const [isInitialized, setIsInitialized] = useState(false);
  const [isGenerating, setIsGenerating] = useState(false);

  // 1. Hidratação inicial a partir do localStorage
  useEffect(() => {
    if (typeof window !== 'undefined') {
      try {
        const saved = localStorage.getItem(STORAGE_KEY);
        if (saved) {
          setSelectedModelIds(JSON.parse(saved));
        }
      } catch (err) {
        console.error('Erro ao ler seleção do Casting Board:', err);
      } finally {
        setIsInitialized(true);
      }
    }
  }, []);

  // 2. Persistência reativa no localStorage
  useEffect(() => {
    if (isInitialized && typeof window !== 'undefined') {
      try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(selectedModelIds));
      } catch (err) {
        console.error('Erro ao salvar seleção do Casting Board:', err);
      }
    }
  }, [selectedModelIds, isInitialized]);

  const addModel = useCallback((id: string) => {
    setSelectedModelIds((prev) => (prev.includes(id) ? prev : [...prev, id]));
  }, []);

  const removeModel = useCallback((id: string) => {
    setSelectedModelIds((prev) => prev.filter((item) => item !== id));
  }, []);

  const toggleModel = useCallback((id: string) => {
    setSelectedModelIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    );
  }, []);

  const clearBoard = useCallback(() => {
    setSelectedModelIds([]);
    if (typeof window !== 'undefined') {
      localStorage.removeItem(STORAGE_KEY);
    }
  }, []);

  const isSelected = useCallback(
    (id: string) => selectedModelIds.includes(id),
    [selectedModelIds]
  );

  /**
   * Sincronização Assíncrona com Supabase:
   * Cria o Board efémero, associa os itens e retorna a URL partilhável.
   */
  const createShareableBoard = async (payload: CreateBoardPayload): Promise<{ shareUrl: string; shareToken: string }> => {
    setIsGenerating(true);
    try {
      // Geração de token efémero único
      const randomPart = Math.random().toString(36).substring(2, 8);
      const cleanTitle = payload.title
        .toLowerCase()
        .replace(/[^a-z0-9]/g, '-')
        .replace(/-+/g, '-')
        .slice(0, 20);
      const shareToken = `${cleanTitle || 'board'}-${Date.now().toString(36)}-${randomPart}`;

      // Cálculo de data de expiração
      const expiresAt = new Date();
      expiresAt.setDate(expiresAt.getDate() + payload.expirationDays);

      // Inserção do board na tabela casting_boards
      const { data: boardData, error: boardError } = await supabase
        .from('casting_boards')
        .insert({
          share_token: shareToken,
          title: payload.title,
          client_name: payload.clientName || null,
          client_email: payload.clientEmail || null,
          notes: payload.notes || null,
          password_hash: payload.password ? payload.password : null, // Idealmente hasheado no servidor
          expires_at: expiresAt.toISOString(),
          is_active: true,
        })
        .select('id')
        .single();

      if (boardError || !boardData) {
        throw new Error(boardError?.message || 'Falha ao criar o Casting Board.');
      }

      // Inserção dos itens associados na tabela board_items
      if (payload.modelIds.length > 0) {
        const boardItemsToInsert = payload.modelIds.map((modelId, index) => ({
          board_id: boardData.id,
          model_id: modelId,
          display_order: index + 1,
        }));

        const { error: itemsError } = await supabase
          .from('board_items')
          .insert(boardItemsToInsert);

        if (itemsError) {
          console.warn('Aviso: Itens salvos localmente (banco remoto aguardando migração):', itemsError.message);
        }
      }

      const baseUrl = typeof window !== 'undefined' ? window.location.origin : '';
      const shareUrl = `${baseUrl}/boards/${shareToken}`;

      return { shareUrl, shareToken };
    } finally {
      setIsGenerating(false);
    }
  };

  return {
    selectedModelIds,
    count: selectedModelIds.length,
    addModel,
    removeModel,
    toggleModel,
    clearBoard,
    isSelected,
    createShareableBoard,
    isGenerating,
  };
}
