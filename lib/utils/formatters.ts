import { EyeColor, HairColor, ModelGender } from '@/types/database.types';

/**
 * Converte altura em cm para formato duplo métrico e imperial (ex: "180 cm / 5'11\"")
 * Padrão essencial para agências de casting internacionais.
 */
export function formatHeight(heightCm: number): string {
  if (!heightCm || isNaN(heightCm)) return '—';
  
  const totalInches = heightCm / 2.54;
  const feet = Math.floor(totalInches / 12);
  const inches = Math.round(totalInches % 12);
  
  return `${Math.round(heightCm)} cm / ${feet}'${inches}"`;
}

/**
 * Formata medidas corporais de modelo em padrão internacional (Busto/Cintura/Quadril)
 */
export function formatMeasurements(bust: number, waist: number, hips: number): string {
  return `${Math.round(bust || 0)}-${Math.round(waist || 0)}-${Math.round(hips || 0)} cm`;
}

/**
 * Converte chaves de enum para rótulos editoriais legíveis
 */
export const EYE_COLOR_LABELS: Record<EyeColor, string> = {
  castanho_claro: 'Castanho Claro',
  castanho_escuro: 'Castanho Escuro',
  verde: 'Verde',
  azul: 'Azul',
  mel: 'Mel',
  preto: 'Preto',
  heterocromia: 'Heterocromia',
};

export const HAIR_COLOR_LABELS: Record<HairColor, string> = {
  preto: 'Preto',
  castanho_escuro: 'Castanho Escuro',
  castanho_claro: 'Castanho Claro',
  loiro: 'Loiro',
  ruivo: 'Ruivo',
  grisalho: 'Grisalho',
  colorido: 'Colorido',
};

export const GENDER_LABELS: Record<ModelGender, string> = {
  female: 'Feminino',
  male: 'Masculino',
  non_binary: 'Não-binário',
};

/**
 * Sanitiza strings para evitar injeção e remove espaços desnecessários
 */
export function sanitizeString(input: string): string {
  if (!input) return '';
  return input
    .trim()
    .replace(/[<>]/g, '') // remove potenciais tags
    .slice(0, 500); // limita tamanho para segurança
}

/**
 * Formata telefone para o padrão E.164 ou WhatsApp link
 */
export function formatWhatsAppLink(phone: string, message: string): string {
  const cleanPhone = phone.replace(/\D/g, '');
  const encodedMsg = encodeURIComponent(message);
  return `https://wa.me/${cleanPhone.startsWith('55') ? cleanPhone : `55${cleanPhone}`}?text=${encodedMsg}`;
}
