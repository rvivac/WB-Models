# WB Casting Intelligence Platform (WB-CIP)

> Plataforma web de alta performance para a agência de modelos **WB Scouting**, projetada sob os padrões de engenharia e estética editorial da **RVIVAC Guild**.

---

## 🏛️ Filosofia de Engenharia: RVIVAC Guild
- **Estética Editorial Minimalista:** Tipografia técnica monoespacada e grotesque de alto contraste.
- **Zero Overengineering:** Eliminação de camadas de backend intermediárias (sem JVM/Spring). Consumo direto do **Supabase (PostgreSQL + RLS + Storage Segregado)**.
- **Core Web Vitals Rigorosos:** Aspect-ratio canônico 3:4 em todas as imagens (CLS = 0.00), carregamento com prioridade LCP e filtragem client-side em memória.
- **Economia de Egress & Quarentena LGPD:** Compressão de fotos no navegador antes do upload via Canvas API (WebP) e descarte automático após 90 dias.

---

## 🛠️ Stack Tecnológica
- **Frontend:** Next.js 14 (App Router), React 18, TypeScript, Tailwind CSS
- **Backend & Persistência:** Supabase (PostgreSQL, Supabase Auth com RLS, Storage com buckets segregados)
- **Edge Functions:** Deno runtime TypeScript para webhooks e integrações serverless (N8N)
- **CDN & Mídia:** Preparado para Cloudflare Images e Cloudflare Stream

---

## 🚀 Como Executar Localmente

```bash
# 1. Instalar as dependências
npm install

# 2. Executar o servidor de desenvolvimento
npm run dev

# 3. Acessar no navegador
# http://localhost:3000
```

---

## 📂 Principais Rotas
- `/`: Home institucional com Hero Stream editorial e vitrine de Stars
- `/casting`: Catálogo público com filtros instantâneos por gênero, altura, manequim e olhos
- `/models/[slug]`: Perfil técnico com biometria, abas e gerador de Composite oficial em A4
- `/scouting`: Funil em 4 etapas para cadastro de novos talentos com conformidade LGPD

---

## 🔒 Segurança e Banco de Dados (Supabase)
O schema completo do PostgreSQL com definições de tabelas, enums, triggers e políticas estritas de Row Level Security (RLS) está documentado em [`supabase/schema.sql`](./supabase/schema.sql).

---

## 📜 Licença e Créditos
Engenharia e Arquitetura de Sistemas desenvolvidas por **RVIVAC Guild Negócios Imobiliários & Soluções Tecnológicas LTDA** (Peruíbe - SP).  
Todos os direitos reservados à **WB Scouting Agency**.
