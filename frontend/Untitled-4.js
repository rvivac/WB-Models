/* ============================================================
 * SCRIPT DE PRE-BUILD: GERA src/environments/version.ts
 * Lê o hash do último commit GIT, branch, data e grava em arquivo TS.
 * Funciona em Windows / Mac / Linux / GitHub Actions.
 * NÃO ALTERAR MANUALMENTE.
 * ============================================================ */
(function () {
  'use strict';

  const fs = require('fs');
  const path = require('path');
  const { execSync } = require('child_process');

  const NL = String.fromCharCode(10);
  const OUT_DIR = path.resolve(__dirname, '..', 'src', 'environments');
  const OUT_FILE = path.join(OUT_DIR, 'version.ts');

  function run(cmd) {
    try {
      return String(execSync(cmd, {
        encoding: 'utf8',
        stdio: ['ignore', 'pipe', 'ignore'],
        timeout: 5000
      })).trim();
    } catch (_err) {
      return '';
    }
  }

  // Captura dados do Git
  let commit = run('git rev-parse HEAD');
  let short = run('git rev-parse --short HEAD');
  let branch = run('git rev-parse --abbrev-ref HEAD');
  const buildDate = new Date().toISOString();

  if (!commit) { commit = 'dev-local'; }
  if (!short)  { short = 'dev'; }
  if (!branch) { branch = 'local'; }

  // Monta o conteúdo do arquivo TypeScript
  const contents =
    '/**' + NL +
    ' * Arquivo GERADO AUTOMATICAMENTE pelo script scripts/prebuild-version.js' + NL +
    ' * NAO EDITE MANUALMENTE. Atualizado SEMPRE antes do ng build.' + NL +
    ' */' + NL +
    'export const APP_VERSION = {' + NL +
    '  commit: ' + JSON.stringify(commit) + ',' + NL +
    '  short: ' + JSON.stringify(short) + ',' + NL +
    '  branch: ' + JSON.stringify(branch) + ',' + NL +
    '  buildDate: ' + JSON.stringify(buildDate) + NL +
    '};' + NL;

  // Garante que pasta existe e escreve arquivo
  fs.mkdirSync(OUT_DIR, { recursive: true });
  fs.writeFileSync(OUT_FILE, contents, 'utf8');

  // Log amigável em PT-BR para o terminal
  console.log('[prebuild] ✅ src/environments/version.ts atualizado:');
  console.log('  commit  = ' + short + '...');
  console.log('  branch  = ' + branch);
  console.log('  build   = ' + new Date(buildDate).toLocaleString('pt-BR', { timeZone: 'America/Sao_Paulo' }));
})();
