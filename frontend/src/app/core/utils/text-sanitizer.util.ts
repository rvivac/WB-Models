/**
 * Utilitário de higienização de textos e reparo de encoding UTF-8
 * Desenvolvido para o backoffice da WB Agency
 */

/**
 * Corrige strings corrompidas por dupla decodificação UTF-8 (ex: "SÃ£o Paulo" -> "São Paulo").
 */
export function fixUtf8(str?: string | null): string {
  if (!str) return '';
  
  let result = str;

  // Tentativa primária: reversão de dupla codificação UTF-8 via decodeURIComponent(escape())
  try {
    if (/[\u00C2-\u00DF][\u0080-\u00BF]/.test(result)) {
      result = decodeURIComponent(escape(result));
    }
  } catch {
    // Se o URI for malformado, fallback para substituição determinística
  }

  // Fallback e salvaguarda para sequências comuns de caracteres acentuados corrompidos em português
  const replacements: [RegExp, string][] = [
    [/SÃ£o/g, 'São'],
    [/Ã£/g, 'ã'],
    [/Ã§/g, 'ç'],
    [/Ã©/g, 'é'],
    [/Ã¡/g, 'á'],
    [/Ã­/g, 'í'],
    [/Ã³/g, 'ó'],
    [/Ãº/g, 'ú'],
    [/Ãª/g, 'ê'],
    [/Ã´/g, 'ô'],
    [/Ã€/g, 'À'],
    [/Ã‰/g, 'É'],
    [/Ã /g, 'Á'],
    [/Ã“/g, 'Ó'],
    [/Ãš/g, 'Ú'],
    [/Ã‚/g, 'Â'],
    [/Ã•/g, 'Õ'],
    [/Ã‡/g, 'Ç']
  ];

  for (const [regex, replacement] of replacements) {
    result = result.replace(regex, replacement);
  }

  return result.trim();
}

/**
 * Higieniza nomes de candidatos para o backoffice:
 * - Corrige acentuação corrompida (UTF-8)
 * - Remove injeções de cabeçalho de e-mail (ex: "Bcc: evil@domain.com", "Cc: ...")
 * - Remove quebras de linha e caracteres de controle
 * - Normaliza espaçamento
 */
export function sanitizeCandidateName(name?: string | null): string {
  if (!name) return '';

  let cleaned = fixUtf8(name);

  // Remove payloads de injeção de email como "Bcc: evil@domain.com" ou "Cc: ..."
  cleaned = cleaned.replace(/\s*(?:bcc|cc):\s*.*$/i, '');

  // Remove caracteres de quebra de linha maliciosos
  cleaned = cleaned.replace(/[\r\n\t]+/g, ' ');

  // Remove eventuais endereços de e-mail avulsos inseridos no campo de nome
  cleaned = cleaned.replace(/\s+<[^>]+@.+>$/g, '');

  return cleaned.trim();
}
