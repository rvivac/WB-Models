import { InstagramUrlPipe, sanitizeInstagramUrl, extractInstagramHandle } from './instagram-url.pipe';

describe('InstagramUrlPipe and Helpers', () => {
  let pipe: InstagramUrlPipe;

  beforeEach(() => {
    pipe = new InstagramUrlPipe();
  });

  describe('sanitizeInstagramUrl', () => {
    it('should return empty string for null, undefined or empty input', () => {
      expect(sanitizeInstagramUrl(null)).toBe('');
      expect(sanitizeInstagramUrl(undefined)).toBe('');
      expect(sanitizeInstagramUrl('')).toBe('');
      expect(sanitizeInstagramUrl('   ')).toBe('');
    });

    it('should normalize @handle to canonical instagram URL', () => {
      expect(sanitizeInstagramUrl('@nomedomodelo')).toBe('https://www.instagram.com/nomedomodelo/');
      expect(sanitizeInstagramUrl('@@nomedomodelo')).toBe('https://www.instagram.com/nomedomodelo/');
    });

    it('should normalize plain handle to canonical instagram URL', () => {
      expect(sanitizeInstagramUrl('nomedomodelo')).toBe('https://www.instagram.com/nomedomodelo/');
    });

    it('should normalize full URLs without www or trailing slash', () => {
      expect(sanitizeInstagramUrl('https://instagram.com/nomedomodelo')).toBe('https://www.instagram.com/nomedomodelo/');
      expect(sanitizeInstagramUrl('http://instagram.com/nomedomodelo/')).toBe('https://www.instagram.com/nomedomodelo/');
      expect(sanitizeInstagramUrl('https://www.instagram.com/nomedomodelo')).toBe('https://www.instagram.com/nomedomodelo/');
      expect(sanitizeInstagramUrl('https://www.instagram.com/nomedomodelo/')).toBe('https://www.instagram.com/nomedomodelo/');
    });

    it('should strip query parameters and hash fragments', () => {
      expect(sanitizeInstagramUrl('https://instagram.com/nomedomodelo?hl=pt-br&utm_source=ig_web')).toBe('https://www.instagram.com/nomedomodelo/');
      expect(sanitizeInstagramUrl('nomedomodelo?ref=badge')).toBe('https://www.instagram.com/nomedomodelo/');
      expect(sanitizeInstagramUrl('https://www.instagram.com/nomedomodelo/#stories')).toBe('https://www.instagram.com/nomedomodelo/');
    });

    it('should handle domain only paths like instagram.com/nomedomodelo', () => {
      expect(sanitizeInstagramUrl('instagram.com/nomedomodelo')).toBe('https://www.instagram.com/nomedomodelo/');
      expect(sanitizeInstagramUrl('www.instagram.com/nomedomodelo/')).toBe('https://www.instagram.com/nomedomodelo/');
    });
  });

  describe('extractInstagramHandle', () => {
    it('should return formatted @handle from handles and urls', () => {
      expect(extractInstagramHandle('nomedomodelo')).toBe('@nomedomodelo');
      expect(extractInstagramHandle('@nomedomodelo')).toBe('@nomedomodelo');
      expect(extractInstagramHandle('https://www.instagram.com/nomedomodelo/')).toBe('@nomedomodelo');
      expect(extractInstagramHandle('')).toBe('');
      expect(extractInstagramHandle(null)).toBe('');
    });
  });

  describe('InstagramUrlPipe transform', () => {
    it('should transform values using sanitizeInstagramUrl', () => {
      expect(pipe.transform('@nomedomodelo')).toBe('https://www.instagram.com/nomedomodelo/');
      expect(pipe.transform('https://instagram.com/nomedomodelo')).toBe('https://www.instagram.com/nomedomodelo/');
      expect(pipe.transform('')).toBe('');
    });
  });
});
