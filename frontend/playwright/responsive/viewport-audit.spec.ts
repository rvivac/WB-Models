import { test, expect } from '@playwright/test';

const TARGET_VIEWPORTS = [
  { name: 'Mobile_SE', width: 375, height: 667 },
  { name: 'Mobile_Common', width: 390, height: 844 },
  { name: 'Tablet_Portrait', width: 768, height: 1024 },
  { name: 'Tablet_Landscape', width: 1024, height: 768 },
  { name: 'Desktop_FHD', width: 1920, height: 1080 }
];

const AUDIT_ROUTES = [
  '/',
  '/seja-modelo',
  '/modelos',
  '/admin/dashboard'
];

test.describe('Validação Rigorosa de Responsividade e Overflow', () => {

  for (const vp of TARGET_VIEWPORTS) {
    for (const route of AUDIT_ROUTES) {
      test(`[${vp.name}] Auditoria em ${route}: Ausência de Scroll Horizontal e Integridade Visual`, async ({ page }) => {
        await page.setViewportSize({ width: vp.width, height: vp.height });

        // Previne que chunks de streaming contínuo de vídeo (.mp4 em loop) impeçam o disparo do networkidle
        await page.route('**/*.mp4', routeReq => routeReq.fulfill({ status: 200, contentType: 'video/mp4', body: '' }));

        // Intercepta chamadas de contagem/dados administrativos para simular sessão ativa e evitar logout defensivo
        await page.route('**/api/v1/admin/**', routeReq => routeReq.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ pending: 5, PENDING: 5, total: 12 })
        }));
        
        // Se a rota for administrativa, injeta estado autenticado simulado no contexto (Cookie e LocalStorage para AuthService)
        if (route.startsWith('/admin')) {
          await page.context().addCookies([
            { name: 'jwt_token', value: 'mock_valid_token_for_layout_test', domain: 'localhost', path: '/' }
          ]);
          await page.addInitScript(() => {
            const mockUser = {
              id: 'admin-01',
              name: 'Administrador Editorial',
              email: 'admin@wbscouting.com',
              role: 'ADMIN'
            };
            const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
            const payload = btoa(JSON.stringify({
              sub: 'admin@wbscouting.com',
              name: 'Administrador Editorial',
              role: 'ADMIN',
              exp: Math.floor(Date.now() / 1000) + 3600 * 24
            }));
            const token = `${header}.${payload}.${btoa('sig')}`;
            window.localStorage.setItem('wb_auth_token', token);
            window.localStorage.setItem('wb_auth_user', JSON.stringify(mockUser));
            window.localStorage.setItem('wb_scouting_token', token);
            window.localStorage.setItem('wb_scouting_user', JSON.stringify(mockUser));
          });
        }

        await page.goto(route, { waitUntil: 'networkidle' });

        // 1. Verificação Estrita de Overflow Horizontal no Documento
        const scrollWidth = await page.evaluate(() => document.documentElement.scrollWidth);
        const clientWidth = await page.evaluate(() => document.documentElement.clientWidth);
        expect(scrollWidth, `Overflow horizontal detectado em ${route} sob resolução ${vp.name}`).toBeLessThanOrEqual(clientWidth);

        // 2. Trava Rígida do Logotipo no Cabeçalho (Se presente)
        const headerLogo = page.locator('header img[alt="WB Agency"]').first();
        if (await headerLogo.isVisible()) {
          const boundingBox = await headerLogo.boundingBox();
          expect(boundingBox).not.toBeNull();
          if (boundingBox) {
            expect(boundingBox.height, 'O logotipo ultrapassou o teto estrito de 32px').toBeLessThanOrEqual(32.5);
          }
        }

        // 3. Inspeção Computada de Border-Radius (Zero Tolerância)
        const hasRoundedElements = await page.evaluate(() => {
          const elements = Array.from(document.querySelectorAll('button, input, select, .card, img'));
          return elements.some(el => {
            const radius = window.getComputedStyle(el).borderRadius;
            return radius !== '0px' && radius !== '';
          });
        });
        expect(hasRoundedElements, `Elemento com border-radius detectado em ${route}`).toBeFalsy();
      });
    }
  }

});
