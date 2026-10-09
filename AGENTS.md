# Diretrizes de Desenvolvimento e Regras Permanentes do Projeto - WB Agency

## 1. SEGURANÇA & UI PÚBLICA (REGRA PERMANENTE E INEGOCIÁVEL)

### 1.1. Proibição de Link de Acesso Restrito no Rodapé ou Áreas Públicas
- **JAMAIS recolocar link para "Acesso Restrito", "Área Restrita", "Admin" ou qualquer direcionamento para login administrativo (`/admin`, `/admin/login`, etc.) no rodapé (`FooterComponent`), no cabeçalho público ou em qualquer parte visível da interface pública.**
- O acesso ao painel administrativo da WB Agency é confidencial e deve ser realizado **exclusivamente via navegação direta na barra de endereços do navegador** digitando a URL `/admin/login`.
- O sub-footer público deve conter exclusivamente:
  - Direitos Autorais (`&copy; [ano] WB Agency. Todos os direitos reservados.`)
  - Créditos editoriais de desenvolvimento (`rvivac guild`)
  - Termos de Uso (modal dinâmico)
  - Privacidade & LGPD (modal dinâmico)

---

## 2. DESIGN SYSTEM & IDENTIDADE VISUAL
- **Estética:** High-fashion, editorial, Light Minimalist.
- **Paleta de Cores:**
  - Fundo principal: `#FFFFFF` e `#FBFBF9`
  - Deep Sage institucional: `#4B584E` (hover: `#38433B`)
  - Linhas e bordas: `#E8EBE8`
  - Textos: `#222624` (títulos e destaque), `#5F6661` (secundário/navegação)
- **Geometria:** `rounded-none` estrito em todos os botões, cards, modais e containers.

---

## 3. INTERNACIONALIZAÇÃO (PT / EN)
- Todas as mensagens e textos do site público devem suportar alternância dinâmica em tempo real (PT e EN).
- Alterações em conteúdos institucionais são gerenciadas no painel bilíngue (`/admin/institucional/idiomas`) e refletidas dinamicamente nas páginas públicas correspondentes.
