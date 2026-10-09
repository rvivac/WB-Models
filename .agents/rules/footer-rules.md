# Regras do Rodapé (FooterComponent) - WB Agency

## Proibição Definitiva de Acesso Restrito no Footer
- **NUNCA recolocar** o link de "Acesso Restrito", "Restricted Access", login ou qualquer rota `/admin` / `/admin/login` no rodapé da aplicação (`FooterComponent`).
- O acesso administrativo não é visível ao público geral e deve ser acessado exclusivamente via URL direta pelo navegador (`/admin/login`).
- As opções legais do sub-footer são estritamente restritas a:
  1. Termos de Uso (aciona modal dinâmico)
  2. Privacidade & LGPD (aciona modal dinâmico)
