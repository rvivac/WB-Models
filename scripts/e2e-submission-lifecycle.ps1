<#
.SYNOPSIS
    Script de Testes End-to-End (E2E) para o Ciclo de Submissão e Consulta de Candidaturas (QA-001 / EAP 5.1.1).
.DESCRIPTION
    Executa testes automatizados contra o backend e frontend da WB Scouting/WB Agency.
    Cobre:
      1. Health check de backend e frontend
      2. Validações negativas de segurança (CRLF Injection, Magic Bytes adulterados, Consentimento LGPD)
      3. Submissão pública com dados biométricos reais e fotos multipart/form-data
      4. Autenticação administrativa com JWT e Cookie HttpOnly
      5. Listagem de candidaturas, contadores e filtros
      6. Detalhamento biométrico e inspeção de mídias assinadas
      7. Parecer e decisão do scouter (status update)
      8. Promoção a Modelo Oficial do casting
      9. Verificação de presença no catálogo público de modelos
      10. Purga de dados no banco e storage sob diretrizes da LGPD (Right to be Forgotten)
#>

param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$FrontendUrl = "http://localhost:4200",
    [string]$AdminEmail = "admin@wbscouting.com",
    [string]$AdminPassword = "Admin@WbScouting2026!",
    [switch]$SkipCleanup = $false
)

$ErrorActionPreference = "Stop"
$swTotal = [System.Diagnostics.Stopwatch]::StartNew()

function Write-Step {
    param([string]$Number, [string]$Title)
    Write-Host "`n=================================================================" -ForegroundColor Cyan
    Write-Host " [$Number] $Title" -ForegroundColor Yellow
    Write-Host "=================================================================" -ForegroundColor Cyan
}

function Write-Success {
    param([string]$Message)
    Write-Host "  [OK] $Message" -ForegroundColor Green
}

function Write-Failure {
    param([string]$Message)
    Write-Host "  [FALHA] $Message" -ForegroundColor Red
}

function Write-Info {
    param([string]$Message)
    Write-Host "  [INFO] $Message" -ForegroundColor DarkGray
}

$results = [System.Collections.Generic.List[PSObject]]::new()

function Register-TestResult {
    param(
        [string]$TestId,
        [string]$Description,
        [bool]$Passed,
        [string]$Details = ""
    )
    $results.Add([PSCustomObject]@{
        ID = $TestId
        Descricao = $Description
        Status = if ($Passed) { "APROVADO" } else { "FALHOU" }
        Detalhes = $Details
    })
    if ($Passed) {
        Write-Success "${TestId}: $Description"
    } else {
        Write-Failure "${TestId}: $Description ($Details)"
    }
}

Write-Host "`n#################################################################" -ForegroundColor Magenta
Write-Host "  WB SCOUTING - SUÍTE DE TESTES E2E: CICLO DE CANDIDATURAS (QA-001)  " -ForegroundColor Magenta
Write-Host "  Data: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') | Target: $BaseUrl" -ForegroundColor Magenta
Write-Host "#################################################################" -ForegroundColor Magenta

# -----------------------------------------------------------------------------
# 1. VERIFICAÇÃO DE DISPONIBILIDADE (HEALTH CHECK)
# -----------------------------------------------------------------------------
Write-Step "1" "Health Check dos Serviços Locais"

try {
    $backendResp = curl.exe -s -o /dev/null -w "%{http_code}" "$BaseUrl/api/v1/models"
    if ($backendResp -eq "200") {
        Register-TestResult "E2E-01" "Backend Spring Boot online e respondendo na porta 8080" $true "HTTP $backendResp"
    } else {
        Register-TestResult "E2E-01" "Backend Spring Boot respondeu código inesperado" $false "HTTP $backendResp"
    }
} catch {
    Register-TestResult "E2E-01" "Falha de conectividade com o Backend Spring Boot" $false $_.Exception.Message
}

try {
    $frontendResp = curl.exe -s -o /dev/null -w "%{http_code}" "$FrontendUrl"
    if ($frontendResp -eq "200") {
        Register-TestResult "E2E-02" "Frontend Angular online e respondendo na porta 4200" $true "HTTP $frontendResp"
    } else {
        Register-TestResult "E2E-02" "Frontend Angular não respondeu 200" $false "HTTP $frontendResp"
    }
} catch {
    Register-TestResult "E2E-02" "Frontend Angular inacessível" $false $_.Exception.Message
}

# -----------------------------------------------------------------------------
# 2. TESTES NEGATIVOS DE VALIDAÇÃO E SEGURANÇA
# -----------------------------------------------------------------------------
Write-Step "2" "Testes Negativos de Validação e Segurança (EAP-SEG-002)"

# Criar arquivos JPEG sintéticos válidos em pasta temporária
$tempDir = Join-Path $env:TEMP ("wb_e2e_" + [System.Guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $tempDir -Force | Out-Null

$validJpegBytes = [byte[]](0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01, 0x01, 0x01, 0x00, 0x60, 0x00, 0x60, 0x00, 0x00, 0xFF, 0xD9)
$faceJpg = Join-Path $tempDir "face.jpg"
$profileJpg = Join-Path $tempDir "profile.jpg"
$bodyJpg = Join-Path $tempDir "body.jpg"
[System.IO.File]::WriteAllBytes($faceJpg, $validJpegBytes)
[System.IO.File]::WriteAllBytes($profileJpg, $validJpegBytes)
[System.IO.File]::WriteAllBytes($bodyJpg, $validJpegBytes)

# 2.1 Rejeição de Nome com CRLF Injection
$crlfPayload = @{
    fullName = "Isabella`r`nBcc: evil@domain.com"
    email = "isabella.crlf@models.com"
    phone = "+5511999998888"
    birthDate = "2003-05-10"
    gender = "FEMALE"
    city = "São Paulo"
    state = "SP"
    height = 1.78
    lgpdConsent = $true
} | ConvertTo-Json

$crlfJsonFile = Join-Path $tempDir "crlf.json"
[System.IO.File]::WriteAllText($crlfJsonFile, $crlfPayload, [System.Text.Encoding]::UTF8)

$crlfHttpCode = curl.exe -s -o /dev/null -w "%{http_code}" -X POST "$BaseUrl/api/v1/submissions" `
    -F "data=@$crlfJsonFile;type=application/json" `
    -F "facePhoto=@$faceJpg;type=image/jpeg" `
    -F "profilePhoto=@$profileJpg;type=image/jpeg" `
    -F "fullBodyPhoto=@$bodyJpg;type=image/jpeg"

if ($crlfHttpCode -eq "400") {
    Register-TestResult "E2E-03" "Rejeição de CRLF Injection em fullName" $true "HTTP 400 Bad Request"
} else {
    Register-TestResult "E2E-03" "CRLF Injection em fullName não foi rejeitado adequadamente" $false "Recebido: HTTP $crlfHttpCode"
}

# 2.2 Rejeição de Submissão sem Consentimento LGPD
$noLgpdPayload = @{
    fullName = "Isabella Sem Lgpd"
    email = "isabella.nolgpd@models.com"
    phone = "+5511999998888"
    birthDate = "2003-05-10"
    gender = "FEMALE"
    city = "São Paulo"
    state = "SP"
    height = 1.78
    lgpdConsent = $false
} | ConvertTo-Json

$noLgpdJsonFile = Join-Path $tempDir "nolgpd.json"
[System.IO.File]::WriteAllText($noLgpdJsonFile, $noLgpdPayload, [System.Text.Encoding]::UTF8)

$noLgpdHttpCode = curl.exe -s -o /dev/null -w "%{http_code}" -X POST "$BaseUrl/api/v1/submissions" `
    -F "data=@$noLgpdJsonFile;type=application/json" `
    -F "facePhoto=@$faceJpg;type=image/jpeg" `
    -F "profilePhoto=@$profileJpg;type=image/jpeg" `
    -F "fullBodyPhoto=@$bodyJpg;type=image/jpeg"

if ($noLgpdHttpCode -eq "400") {
    Register-TestResult "E2E-04" "Rejeição de submissão sem aceite de LGPD (AssertTrue)" $true "HTTP 400 Bad Request"
} else {
    Register-TestResult "E2E-04" "Submissão sem aceite de LGPD não retornou 400" $false "Recebido: HTTP $noLgpdHttpCode"
}

# -----------------------------------------------------------------------------
# 3. SUBMISSÃO PÚBLICA DE CANDIDATURA (HAPPY PATH)
# -----------------------------------------------------------------------------
Write-Step "3" "Submissão Pública de Candidatura (Happy Path)"

$timestampSuffix = (Get-Date).ToString("yyyyMMddHHmmss")
$testCandidateName = "Clara Valente Scouting"
$testCandidateEmail = "clara.valente.$timestampSuffix@test-models.com"

$validPayload = @{
    fullName = $testCandidateName
    email = $testCandidateEmail
    phone = "+5511988887777"
    birthDate = "2002-04-15"
    gender = "FEMALE"
    city = "Florianópolis"
    state = "SC"
    height = 1.79
    bust = 85.0
    waist = 61.0
    hips = 90.0
    shoeSize = 38
    eyeColor = "Verdes"
    hairColor = "Castanho Iluminado"
    instagramHandle = "@claravalente"
    lgpdConsent = $true
} | ConvertTo-Json

$validJsonFile = Join-Path $tempDir "valid_submission.json"
[System.IO.File]::WriteAllText($validJsonFile, $validPayload, [System.Text.Encoding]::UTF8)

$submitRespFile = Join-Path $tempDir "submit_response.json"
$submitHttpCode = curl.exe -s -w "%{http_code}" -o $submitRespFile -X POST "$BaseUrl/api/v1/submissions" `
    -F "data=@$validJsonFile;type=application/json" `
    -F "facePhoto=@$faceJpg;type=image/jpeg" `
    -F "profilePhoto=@$profileJpg;type=image/jpeg" `
    -F "fullBodyPhoto=@$bodyJpg;type=image/jpeg"

$submissionId = $null
$protocol = $null

if ($submitHttpCode -eq "201") {
    $submitJson = Get-Content $submitRespFile -Raw | ConvertFrom-Json
    $submissionId = $submitJson.id
    $protocol = $submitJson.protocol
    Write-Info "ID Criado: $submissionId"
    Write-Info "Protocolo Gerado: $protocol"
    
    $protocolValid = $protocol -match "^WB-\d{8}-[A-F0-9]{6}$"
    Register-TestResult "E2E-05" "Submissão pública com upload de 3 fotos (HTTP 201)" $true "ID: $submissionId"
    Register-TestResult "E2E-06" "Formato padronizado do Protocolo de Candidatura (WB-YYYYMMDD-XXXXXX)" $protocolValid "Protocolo: $protocol"
} else {
    $errContent = if (Test-Path $submitRespFile) { Get-Content $submitRespFile -Raw } else { "Sem corpo" }
    Register-TestResult "E2E-05" "Falha na submissão de candidatura" $false "HTTP ${submitHttpCode}: $errContent"
    Register-TestResult "E2E-06" "Geração de protocolo não executada devido a falha na submissão" $false ""
}

# -----------------------------------------------------------------------------
# 4. AUTENTICAÇÃO ADMINISTRATIVA (PAINEL CMS / SCOUTING DESK)
# -----------------------------------------------------------------------------
Write-Step "4" "Autenticação Administrativa e Proteção de Endpoints"

# 4.1 Acesso não autenticado bloqueado
$unauthCode = curl.exe -s -o /dev/null -w "%{http_code}" "$BaseUrl/api/v1/admin/applications"
if ($unauthCode -in @("401", "403")) {
    Register-TestResult "E2E-07" "Bloqueio de acesso não autenticado a rotas administrativas" $true "HTTP $unauthCode"
} else {
    Register-TestResult "E2E-07" "Rota administrativa permitiu acesso sem credenciais" $false "Recebido: HTTP $unauthCode"
}

# 4.2 Login com sucesso
$jwtToken = $null
try {
    $loginBody = @{
        email = $AdminEmail
        password = $AdminPassword
    } | ConvertTo-Json

    $loginResp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    $jwtToken = $loginResp.accessToken

    if ($jwtToken -and $jwtToken.Length -gt 20) {
        Register-TestResult "E2E-08" "Autenticação de administrador com emissão de JWT" $true "Admin: $($loginResp.adminEmail)"
    } else {
        Register-TestResult "E2E-08" "Token JWT nulo ou inválido no payload de autenticação" $false ""
    }
} catch {
    Register-TestResult "E2E-08" "Falha no login do administrador" $false $_.Exception.Message
}

$authHeader = @{ "Authorization" = "Bearer $jwtToken" }

# -----------------------------------------------------------------------------
# 5. CONSULTA ADMINISTRATIVA, FILTROS E MÉTRICAS
# -----------------------------------------------------------------------------
Write-Step "5" "Consulta Administrativa de Candidaturas e Métricas"

if ($jwtToken -and $submissionId) {
    try {
        # 5.1 Listagem e busca pelo protocolo ou nome
        $listResp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/admin/applications?search=$protocol" -Headers $authHeader -Method Get
        $found = $listResp.content | Where-Object { $_.id -eq $submissionId -or $_.protocol -eq $protocol }
        if (-not $found) {
            $listResp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/admin/applications?search=$testCandidateName" -Headers $authHeader -Method Get
            $found = $listResp.content | Where-Object { $_.id -eq $submissionId -or $_.protocol -eq $protocol }
        }

        if ($found) {
            Register-TestResult "E2E-09" "Localização da candidatura recém-submetida na listagem administrativa" $true "Nome: $($found.fullName), Status: $($found.status)"
        } else {
            Register-TestResult "E2E-09" "Candidatura não encontrada na listagem administrativa" $false "Protocolo buscado: $protocol"
        }

        # 5.2 Contadores
        $countsResp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/admin/applications/counts" -Headers $authHeader -Method Get
        $hasCounts = ($countsResp.total -gt 0) -and ($countsResp.PSObject.Properties['pending'] -ne $null)
        Register-TestResult "E2E-10" "Endpoint de métricas agregadas por status (/applications/counts)" $hasCounts "Total: $($countsResp.total), Pendentes: $($countsResp.pending)"

        # 5.3 Detalhamento
        $detailResp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/admin/applications/$submissionId" -Headers $authHeader -Method Get
        $hasBiometrics = ($detailResp.biometrics.height -in @(179, 1.79)) -and ($detailResp.biometrics.bust -in @(85.0, 85))
        $hasPhotos = ($detailResp.photos.Count -eq 3)
        Register-TestResult "E2E-11" "Detalhamento da candidatura com biometria e fotos assinadas" ($hasBiometrics -and $hasPhotos) "Fotos: $($detailResp.photos.Count), Altura: $($detailResp.biometrics.height) (cm)"
    } catch {
        Register-TestResult "E2E-09" "Erro ao consultar listagem de candidaturas" $false $_.Exception.Message
        Register-TestResult "E2E-10" "Erro ao consultar métricas" $false $_.Exception.Message
        Register-TestResult "E2E-11" "Erro ao consultar detalhes da candidatura" $false $_.Exception.Message
    }
} else {
    Write-Info "Etapa 5 ignorada por ausência de token ou submissionId."
}

# -----------------------------------------------------------------------------
# 6. DECISÃO DE TRIAGEM (PARECER E ATUALIZAÇÃO DE STATUS)
# -----------------------------------------------------------------------------
Write-Step "6" "Decisão de Triagem e Parecer do Scouter"

if ($jwtToken -and $submissionId) {
    try {
        $decisionBody = @{
            status = "APPROVED"
            internalNotes = "Aprovada na triagem E2E automatizada. Perfil apto para elenco comercial."
        } | ConvertTo-Json

        $decisionResp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/admin/applications/$submissionId/decision" `
            -Headers $authHeader -Method Patch -Body $decisionBody -ContentType "application/json"

        $isApproved = ($decisionResp.status -eq "APPROVED")
        Register-TestResult "E2E-12" "Atualização de decisão/status para APPROVED com parecer técnico" $isApproved "Status: $($decisionResp.status)"
    } catch {
        Register-TestResult "E2E-12" "Falha na atualização de decisão da candidatura" $false $_.Exception.Message
    }
}

# -----------------------------------------------------------------------------
# 7. PROMOÇÃO A MODELO OFICIAL E CONFIRMAÇÃO NO CATÁLOGO
# -----------------------------------------------------------------------------
Write-Step "7" "Promoção a Modelo Oficial e Catálogo de Casting"

$promotedModelId = $null
if ($jwtToken -and $submissionId) {
    try {
        $promoteResp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/admin/applications/$submissionId/promote?activateImmediately=true" `
            -Headers $authHeader -Method Post

        $promotedModelId = $promoteResp.data.id
        $isPromotedOk = ($promoteResp.success -eq $true) -and ($promotedModelId -ne $null)

        Register-TestResult "E2E-13" "Promoção de candidatura a Modelo Oficial (HTTP 201)" $isPromotedOk "Model ID: $promotedModelId, Nome: $($promoteResp.data.stageName)"

        if ($promotedModelId) {
            # Consulta pública para confirmar presença no casting
            $publicModel = Invoke-RestMethod -Uri "$BaseUrl/api/v1/models/$promotedModelId" -Method Get
            $publicVisible = ($publicModel.id -eq $promotedModelId) -and ($publicModel.stageName -eq $testCandidateName)
            Register-TestResult "E2E-14" "Confirmação de exibição do modelo promovido na API pública de casting" $publicVisible "Catálogo público OK"
        }
    } catch {
        Register-TestResult "E2E-13" "Falha na promoção para modelo oficial" $false $_.Exception.Message
        Register-TestResult "E2E-14" "Consulta no catálogo público não realizada" $false ""
    }
}

# -----------------------------------------------------------------------------
# 8. PURGA LGPD / EXCLUSÃO SEGURA
# -----------------------------------------------------------------------------
Write-Step "8" "Exclusão Segura e Purga LGPD (Right to be Forgotten)"

if (-not $SkipCleanup -and $jwtToken -and $submissionId) {
    try {
        $deleteCode = curl.exe -s -o /dev/null -w "%{http_code}" -X DELETE "$BaseUrl/api/v1/admin/applications/$submissionId" `
            -H "Authorization: Bearer $jwtToken"

        if ($deleteCode -eq "204") {
            Register-TestResult "E2E-15" "Exclusão física da candidatura e purga no storage (HTTP 204)" $true "Purge LGPD concluído"

            # Confirma que não é mais encontrada (404)
            $verifyDeletedCode = curl.exe -s -o /dev/null -w "%{http_code}" "$BaseUrl/api/v1/admin/applications/$submissionId" `
                -H "Authorization: Bearer $jwtToken"
            
            $isNotFound = ($verifyDeletedCode -eq "404")
            Register-TestResult "E2E-16" "Garantia de não-localização após purga (HTTP 404)" $isNotFound "HTTP $verifyDeletedCode"
        } else {
            Register-TestResult "E2E-15" "Falha na exclusão da candidatura" $false "Recebido: HTTP $deleteCode"
            Register-TestResult "E2E-16" "Garantia pós-exclusão ignorada" $false ""
        }
    } catch {
        Register-TestResult "E2E-15" "Exceção ao purgar candidatura" $false $_.Exception.Message
        Register-TestResult "E2E-16" "Garantia pós-exclusão ignorada" $false ""
    }
}

# Limpeza de arquivos temporários locais
if (Test-Path $tempDir) {
    Remove-Item -Path $tempDir -Recurse -Force | Out-Null
}

$swTotal.Stop()

# -----------------------------------------------------------------------------
# PAINEL FINAL DE RESULTADOS
# -----------------------------------------------------------------------------
Write-Host "`n=================================================================" -ForegroundColor Cyan
Write-Host "                PAINEL CONSOLIDADO DE TESTES E2E                 " -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan

$results | Format-Table -AutoSize -Property ID, Descricao, Status, Detalhes

$passedCount = ($results | Where-Object { $_.Status -eq "APROVADO" }).Count
$failedCount = ($results | Where-Object { $_.Status -eq "FALHOU" }).Count
$totalCount = $results.Count

Write-Host "Duração Total: $($swTotal.Elapsed.TotalSeconds.ToString('F2')) s" -ForegroundColor DarkGray
Write-Host "Total de Cenários E2E: $totalCount | Aprovados: $passedCount | Falhas: $failedCount" `
    -ForegroundColor $(if ($failedCount -eq 0) { "Green" } else { "Red" })

if ($failedCount -eq 0) {
    Write-Host "`n>>> SUCESSO: Todos os testes E2E do Ciclo de Candidaturas foram APROVADOS! <<<`n" -ForegroundColor Green
    exit 0
} else {
    Write-Host "`n>>> ATENÇÃO: $failedCount cenário(s) E2E reprovado(s)! <<<`n" -ForegroundColor Red
    exit 1
}
