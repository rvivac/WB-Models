import { fixUtf8, sanitizeCandidateName } from './text-sanitizer.util';

describe('TextSanitizerUtil', () => {
  describe('fixUtf8', () => {
    it('should fix double UTF-8 encoded strings', () => {
      expect(fixUtf8('SÃ£o Paulo')).toBe('São Paulo');
      expect(fixUtf8('BrasÃ­lia')).toBe('Brasília');
      expect(fixUtf8('RibeirÃ£o Preto')).toBe('Ribeirão Preto');
      expect(fixUtf8('CuiabÃ¡')).toBe('Cuiabá');
      expect(fixUtf8('BelÃ©m')).toBe('Belém');
      expect(fixUtf8('GoiÃ¢nia')).toBe('Goiânia');
    });

    it('should preserve correctly encoded strings', () => {
      expect(fixUtf8('São Paulo')).toBe('São Paulo');
      expect(fixUtf8('Rio de Janeiro')).toBe('Rio de Janeiro');
      expect(fixUtf8('Curitiba')).toBe('Curitiba');
    });

    it('should handle empty or null values gracefully', () => {
      expect(fixUtf8('')).toBe('');
      expect(fixUtf8(null)).toBe('');
      expect(fixUtf8(undefined)).toBe('');
    });
  });

  describe('sanitizeCandidateName', () => {
    it('should strip Bcc / Cc email injection attempts from candidate names', () => {
      expect(sanitizeCandidateName('Isabella Bcc: evil@domain.com')).toBe('Isabella');
      expect(sanitizeCandidateName('Carlos Cc: hacker@test.org')).toBe('Carlos');
      expect(sanitizeCandidateName('Mariana bcc:spam@domain.com')).toBe('Mariana');
    });

    it('should clean control characters and newlines', () => {
      expect(sanitizeCandidateName('Helena\r\nSilva')).toBe('Helena Silva');
      expect(sanitizeCandidateName('Pedro\tSantos')).toBe('Pedro Santos');
    });

    it('should fix UTF-8 while sanitizing name', () => {
      expect(sanitizeCandidateName('JoÃ£o Bcc: test@evil.com')).toBe('João');
      expect(sanitizeCandidateName('VitÃ³ria')).toBe('Vitória');
    });

    it('should handle standard clean names correctly', () => {
      expect(sanitizeCandidateName('Camila Rodrigues Mendes')).toBe('Camila Rodrigues Mendes');
      expect(sanitizeCandidateName('Lucas Gabriel Silveira')).toBe('Lucas Gabriel Silveira');
    });
  });
});
