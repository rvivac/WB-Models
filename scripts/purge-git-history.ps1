<#
.SYNOPSIS
    Script de higienização de histórico Git usando git-filter-repo (EAP-SEG-002 Item 5.1).
.DESCRIPTION
    Remove ocorrências de senhas antigas e segredos de commits históricos.
    REQUER: python -m pip install --user git-filter-repo
    ATENÇÃO: Operação destrutiva que reescreve hashes SHA-1 de commits.
             Execute apenas com confirmação da equipe antes do force-push coordenado.
#>

param(
    [switch]$Force = $false
)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " WB AGENCY - HIGIENIZAÇÃO DE HISTÓRICO GIT (EAP-SEG-002) " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$expressionsFile = Join-Path $PSScriptRoot "expressions.txt"
if (-not (Test-Path $expressionsFile)) {
    Write-Error "Arquivo expressions.txt não encontrado em $expressionsFile"
    exit 1
}

if (-not $Force) {
    Write-Warning "Este script reescreverá o histórico local do Git."
    Write-Warning "Todos os colaboradores precisarão clonar ou resetar suas branches após o force push."
    $confirm = Read-Host "Deseja continuar? Digite 'SIM' para confirmar"
    if ($confirm -ne "SIM") {
        Write-Host "Operação abortada pelo operador." -ForegroundColor Yellow
        exit 0
    }
}

Write-Host "Verificando se git-filter-repo está disponível..." -ForegroundColor Green
$gitFilterRepo = Get-Command "git-filter-repo" -ErrorAction SilentlyContinue

if (-not $gitFilterRepo) {
    Write-Host "git-filter-repo não encontrado. Instalando via pip..." -ForegroundColor Yellow
    python -m pip install --user git-filter-repo
}

Write-Host "Executando purga no histórico Git..." -ForegroundColor Green
git filter-repo --replace-text $expressionsFile --force

Write-Host "`nHistórico higienizado com sucesso!" -ForegroundColor Green
Write-Host "Para sincronizar com o repositório remoto (após alinhamento):" -ForegroundColor Cyan
Write-Host "  git push origin --force --all" -ForegroundColor White
Write-Host "  git push origin --force --tags" -ForegroundColor White
