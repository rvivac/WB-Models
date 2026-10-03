# Auditoria Completa de Consistência do Supabase - PRD

## Overview
- **Summary**: Realizar auditoria end-to-end no uso e acesso ao Supabase em todo o ecossistema WB Scouting, cobrindo inicialização de clientes, credenciais em múltiplos ambientes, políticas RLS em tabelas e storage, padronização de operações CRUD, testes de conectividade, e documentação das correções aplicadas.
- **Purpose**: Eliminar inconsistências de configuração, fechar lacunas de segurança (RLS/credenciais), padronizar o acesso programático e garantir que a integração funcione uniformemente em desenvolvimento, homologação e produção.
- **Target Users**: Equipe de desenvolvimento e DevOps do projeto WB Scouting / WB Agency.

## Goals
1. Garantir **uma única instância compartilhada** do cliente/bean de acesso ao Supabase Storage em todo o backend Java.
2. Uniformizar o carregamento das credenciais (URL, anon key, service role key) e corrigir bugs na lógica de fallback de chave efetiva.
3. Garantir aplicação uniforme de RLS em **todas** as tabelas do schema public e nos buckets de storage, sem lacunas para roles `anon`.
4. Padronizar nomes de buckets, limites de upload, validações MIME e tratamento de erros em todos os serviços que acessam Storage.
5. Consolidar scripts DDL (schema.sql + migrations) para eliminar duplicações e definições conflitantes.
6. Executar testes de conectividade e autenticação com credenciais de ambiente.
7. Documentar relatório completo de inconsistências e correções implementadas.

## Non-Goals
- Não alterar a arquitetura Spring Boot / JPA para uso de PostgREST (a persistência transacional continua via Spring Data JPA + JDBC PostgreSQL).
- Não implementar novos módulos de negócio.
- Não refatorar o frontend Angular para integração direta com Supabase JS client (permanece BFF via API REST Spring Boot).
- Não rotacionar chaves de produção; apenas padronizar carregamento e fallback.

## Background & Context
- Repositório: WB Models (backend Spring Boot 3.4 + PostgreSQL Supabase + frontend Angular).
- Acesso a dados relacional: Spring Data JPA/Hibernate via JDBC PostgreSQL direto com HikariCP.
- Acesso a Storage API: RestClient Spring customizado com bean único em `StorageConfig`.
- Foram identificadas nas explorações iniciais:
  - **Bug crítico** em `SupabaseProperties.getEffectiveKey()`: após verificar `serviceRoleKey` com dummy-check e cair adiante, a mesma chave dummy é retornada na terceira checagem sem o `isDummy`, quebrando a semântica.
  - **DDL duplicado e conflitante**: Tabela `candidate_submissions` definida duas vezes em `schema.sql` (linhas 212 e 377) com colunas e defaults diferentes.
  - **Lógica de bucket duplicada**: `AdminCandidateServiceImpl`, `AdminCandidateQueryServiceImpl`, `SiteContentServiceImpl` têm cada um seu método `resolveBucketName()` com default hardcoded.
  - **Validações de upload DRY-violating**: `SupabaseStorageService`, `SiteContentServiceImpl` e `CandidateSubmissionServiceImpl` validam tamanho/MIME separadamente com limites diferentes.
  - **Role least-privilege criada mas não utilizada**: `wb_app_user` existe na migration mas `application.yml` conecta como owner `postgres.xxx`.
  - **Defaults de produção hardcoded** em `application.yml` (URL do Supabase, DB_USERNAME, senha admin inicial).

## Functional Requirements
- **FR-1**: Inicialização única de cliente Supabase Storage RestClient via bean Spring singleton; nenhum outro ponto do código deve instanciar RestClient/Cliente HTTP próprio para Storage.
- **FR-2**: Resolução de chave efetiva `getEffectiveKey()` em ordem de prioridade: serviceRoleKey (não-dummy) → key (não-dummy) → anonKey (não-dummy) → lançar estado explícito de não-configurado; sem retornar "dummy-key" em ramos indevidos.
- **FR-3**: Prover métodos centralizados em `SupabaseProperties` para resolver nomes de buckets (sem duplicação nos serviços) e validar que um bucket nomeado pertence ao conjunto permitido.
- **FR-4**: Eliminação de duplicações em `schema.sql`: tabela `candidate_submissions` deve ter uma única definição canônica que inclua colunas extras (`converted_to_model_id`, reviewed_*, etc.) e triggers padronizados de updated_at.
- **FR-5**: RLS habilitado e com policies explícitas em todas as tabelas públicas; nenhuma policy de escrita para role `anon` em nenhuma tabela ou storage.objects.
- **FR-6**: Validações de upload (MIME type + tamanho) centralizadas em uma única classe (StorageService ou componente dedicado); todos os serviços devem usar a mesma validação.
- **FR-7**: Relatório final de inconsistências e correções entregue como artefato da especificação.

## Non-Functional Requirements
- **NFR-1 (Segurança)**: Nenhuma credencial padrão hardcoded sensível em `application.yml` de produção; defaults devem ser vazios com fallbacks claros.
- **NFR-2 (Portabilidade)**: A aplicação deve funcionar corretamente configurando apenas `.env` ou variáveis de ambiente; não depender de IDs de projeto embutidos.
- **NFR-3 (Determinismo)**: `getEffectiveKey()` deve ser pura e idempotente; chamadas sucessivas retornam o mesmo resultado.
- **NFR-4 (Observabilidade)**: Toda falha de comunicação com Supabase (storage/rest) deve logar com contexto de bucket, path, status HTTP e código de erro estruturado.
- **NFR-5 (Testabilidade)**: Testes unitários existentes (`SupabaseRlsHardeningTest`, `DatabaseConnectionTest`, `EnvironmentConfigurationInitializationTest`) devem continuar passando após as correções.

## Constraints
- **Technical**: Java 21, Spring Boot 3.4.x, PostgreSQL 14+ via Supabase, nenhuma nova dependência de biblioteca externa além das já presentes em `pom.xml`.
- **Business**: Não quebrar compatibilidade com endpoints públicos (`/submissions`, catálogo de modelos) nem com painel admin existente.
- **Dependencies**: Migrations DDL aplicáveis em qualquer ordem relativa desde que idempotentes (CREATE IF NOT EXISTS / DROP IF EXISTS).

## Assumptions
- A aplicação Spring conecta ao PostgreSQL como owner/dono do schema (com bypass RLS por privilégio); policies RLS defendem as camadas PostgREST/Storage e acessos anônimos, não a conexão JDBC principal.
- O frontend não tem `@supabase/supabase-js` e não se conecta diretamente — padrão BFF mantido.
- Testes que exigem conexão real ao Supabase só serão executados se `DB_PASSWORD` estiver presente; sem essa var, testes de conexão são abortados com status informativo, não de falha.

## Acceptance Criteria

### AC-1: Instância única do cliente Storage e propriedades centralizadas
- **Type**: `rule`
- **Given**: O código fonte Java do backend
- **When**: É feita busca por `new RestClient` ou `RestClient.builder()` no pacote de storage/fora de StorageConfig
- **Then**: Apenas `StorageConfig.supabaseStorageRestClient()` contém a construção do RestClient de storage; todos os serviços injetam o mesmo bean via `@RequiredArgsConstructor`/`@Autowired`
- **Pass Condition**: grep por `RestClient.builder` em src/main/java retorna exatamente 1 ocorrência em StorageConfig.java
- **Evidence**: Saída do grep + inspeção de SupabaseStorageService recebendo RestClient via construtor

### AC-2: Resolução correta da chave efetiva sem dummy false-positive
- **Type**: `rule`
- **Given**: SupabaseProperties com diferentes combinações de valores
- **When**: serviceRoleKey = "dummy-key" e key = "chave-real-valida"
- **Then**: `getEffectiveKey()` retorna "chave-real-valida" (e NÃO "dummy-key")
- **Pass Condition**: Teste unitário Java com @TestParameter para a combinação acima passa, juntamente com casos serviceRoleKey válida > key válida > anonKey válida > todas dummy (isKeyConfigured=false)
- **Evidence**: Resultado de `mvn -pl backend -Dtest=SupabasePropertiesEffectiveKeyTest test` (teste novo ou existente)

### AC-3: Resolução centralizada de buckets sem duplicação
- **Type**: `rule`
- **Given**: Services que acessam Storage
- **When**: É verificado o código de AdminCandidateServiceImpl, AdminCandidateQueryServiceImpl, SiteContentServiceImpl, ModelMediaServiceImpl, CandidateSubmissionServiceImpl
- **Then**: Nenhum deles contém método `resolveBucketName()` próprio nem constantes de bucket DEFAULT_BUCKET duplicadas; todos usam `SupabaseProperties.Buckets#getters` ou um método utilitário único em `SupabaseProperties`
- **Pass Condition**: grep por "DEFAULT_BUCKET\|resolveBucketName\|@Value.*bucket" retorna 0 ocorrências fora de SupabaseProperties
- **Evidence**: Saída do grep + diff mostrando remoção dos métodos duplicados

### AC-4: schema.sql com tabelas canônicas únicas e sem conflitos
- **Type**: `rule`
- **Given**: Arquivo `supabase/schema.sql`
- **When**: É aplicado parse/simulação do script ou grep por `CREATE TABLE IF NOT EXISTS public.candidate_submissions`
- **Then**: Existe exatamente **1** definição da tabela `candidate_submissions`, contendo colunas: protocol, reviewed_by, reviewed_at, feedback_notes, converted_to_model_id, lgpd_consent + lgpd_consent_at
- **Pass Condition**: Contagem grep por `CREATE TABLE IF NOT EXISTS public.candidate_submissions` == 1 e colunas requeridas presentes
- **Evidence**: Saída do grep + definição única em schema.sql

### AC-5: RLS em todas as tabelas sem policies anônimas de escrita
- **Type**: `rule`
- **Given**: Todos os scripts SQL (schema.sql + migrations)
- **When**: É executado o `SupabaseRlsHardeningTest` no banco configurado ou auditoria estática local
- **Then**:
  1. `ALTER TABLE x ENABLE ROW LEVEL SECURITY` aparece para: admins, models, model_media, candidates, candidate_photos, candidate_submissions, site_contents, admin_audit_logs
  2. Nenhuma policy em `pg_policies` (storage.objects ou schema public) contém role `anon` + cmd em (INSERT, UPDATE, DELETE)
- **Pass Condition**: Teste SupabaseRlsHardeningTest passa ou auditoria estática valida as condições
- **Evidence**: Relatório do teste ou saída SQL de pg_policies filtrada

### AC-6: Validações de upload centralizadas (tamanho e MIME)
- **Type**: `rule`
- **Given**: Fluxos de upload de Storage (candidatos, mídia de modelos, assets institucionais)
- **When**: Um serviço Java realiza upload via StorageService
- **Then**: Não há revalidação redundante de tamanho/MIME no chamador; a validação centralizada em `SupabaseStorageService.validateUpload` (ou classe dedicada) é o único ponto que lança `FileSizeExceededException` / `InvalidFileException` por tipo/tamanho
- **Pass Condition**: grep por "validateAssetFile\|validateImageFile\|MAX_FILE_SIZE\|ALLOWED_IMAGE_TYPES" em arquivos de **serviço** (fora SupabaseStorageService) retorna zero ou apenas delegação clara ao StorageService
- **Evidence**: Saída do grep + inspeção dos serviços removendo duplicatas

### AC-7: Sem credenciais hardcoded sensíveis em application.yml
- **Type**: `rule`
- **Given**: backend/src/main/resources/application.yml
- **When**: O arquivo é inspecionado nas chaves `supabase.url`, `spring.datasource.username`, `app.security.initial-admin.password`
- **Then**:
  - `supabase.url` default é `${SUPABASE_URL:}` (vazio, sem projeto embutido)
  - `spring.datasource.username` default é `${DB_USERNAME:}` (vazio)
  - `app.security.initial-admin.password` default é `${INITIAL_ADMIN_PASSWORD:}` (vazio) ou documentado como "somente desenvolvimento"
- **Pass Condition**: Os defaults não contêm valores como "stmytwsdlonpnirqiufq", senhas literais, ou e-mails fixos de produção; `.env.example` e testes continuam provendo placeholders
- **Evidence**: Diff do application.yml após a alteração

### AC-8: Cobertura e qualidade do relatório
- **Type**: `rubric`
- **Dimension**: Exaustividade e clareza do relatório final de auditoria
- **Scale**: 0-2
- **Anchors**: 0 = relatório vago sem apontar inconsistências concretas; 1 = inconsistências listadas, mas sem link para arquivos/locais corrigidos; 2 = cada inconsistência tem descrição, impacto, arquivo/linha afetada e link para a correção aplicada
- **Pass Threshold**: >= 2
- **Evidence**: Documentação final (no spec/review ou relatório em tasks) com cada item encontrado e corrigido, com referências a trechos de código

### AC-9: Testes de configuração existentes passando
- **Type**: `rule`
- **Given**: Ambiente de compilação local com JDK 21 e Maven
- **When**: Executado `mvn -pl backend test -Dtest=EnvironmentConfigurationInitializationTest,SupabaseStorageServiceTest,CandidateSubmissionFallbackTest`
- **Then**: Todos os testes passam sem falhas de compilação ou runtime
- **Pass Condition**: exit code 0 do Maven + relatório de testes verde
- **Evidence**: Saída do comando Maven com os testes especificados

## Open Questions
- [ ] O papel `wb_app_user` deve ser adotado em produção agora ou a migração de conexão para least-privilege fica como roadmap futuro (registrada no relatório como recomendação)?
- [ ] O ID `stmytwsdlonpnirqiufq` hardcoded é o projeto real e pode ser removido dos defaults, corrigindo `application.yml` para placeholders vazios?
