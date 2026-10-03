# Script de preparação e empacotamento do Frontend para a Hostinger
# 1. Compila o Angular com a configuração de produção
# 2. Gera 404.html e copia .htaccess para suporte a SPA (rotas do Angular no Apache)
# 3. Atualiza a pasta deploy-wbagency e gera o arquivo deploy-hostinger.zip na raiz

Write-Host "===> 1. Compilando o Frontend em modo de produção..." -ForegroundColor Cyan
Set-Location -Path "$PSScriptRoot/../frontend"
npm run build -- --configuration production

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERRO: A compilação do Angular falhou!" -ForegroundColor Red
    exit 1
}

$distBrowser = "$PSScriptRoot/../frontend/dist/frontend/browser"
$deployDir = "$PSScriptRoot/../deploy-wbagency"
$zipPath = "$PSScriptRoot/../deploy-hostinger.zip"

Write-Host "===> 2. Configurando roteamento SPA (404.html e .htaccess)..." -ForegroundColor Cyan
Copy-Item "$distBrowser/index.html" "$distBrowser/404.html" -Force

$htaccessContent = @"
DirectoryIndex index.html index.php
<IfModule mod_rewrite.c>
  RewriteEngine On
  RewriteBase /
  RewriteRule ^index\.html$ - [L]
  RewriteCond %{REQUEST_FILENAME} !-f
  RewriteCond %{REQUEST_FILENAME} !-d
  RewriteRule . /index.html [L]
</IfModule>
"@
Set-Content -Path "$distBrowser/.htaccess" -Value $htaccessContent -Encoding UTF8

Write-Host "===> 3. Atualizando pasta deploy-wbagency..." -ForegroundColor Cyan
Copy-Item "$distBrowser/*" "$deployDir/" -Recurse -Force
Copy-Item "$distBrowser/.htaccess" "$deployDir/.htaccess" -Force

Write-Host "===> 4. Gerando pacote compactado deploy-hostinger.zip..." -ForegroundColor Cyan
if (Test-Path $zipPath) {
    Remove-Item $zipPath -Force
}
Compress-Archive -Path "$distBrowser/*", "$distBrowser/.htaccess" -DestinationPath $zipPath -Force

Write-Host "===> SUCESSO! Pacote deploy-hostinger.zip gerado na raiz do projeto." -ForegroundColor Green
Write-Host "Basta enviar o deploy-hostinger.zip para a pasta public_html da Hostinger e clicar em 'Extrair'." -ForegroundColor Yellow
Set-Location -Path "$PSScriptRoot/.."
