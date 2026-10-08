# Script de preparação e empacotamento do Frontend para a Hostinger
# 1. Compila o Angular com a configuração de produção
# 2. Gera 404.html e copia .htaccess com UTF-8 sem BOM (compatível Apache)
# 3. Atualiza a pasta deploy-wbagency limpa
# 4. Gera deploy-hostinger.zip com separadores Linux '/' (100% compatível com hPanel Hostinger)

$ErrorActionPreference = 'Stop'

Write-Host "===> 1. Compilando o Frontend em modo de produção..." -ForegroundColor Cyan
Set-Location -Path "$PSScriptRoot/../frontend"
npm run build:prod

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERRO: A compilação do Angular falhou!" -ForegroundColor Red
    exit 1
}

$distBrowser = "$PSScriptRoot/../frontend/dist/frontend/browser"
$deployDir = "$PSScriptRoot/../deploy-wbagency"
$zipPath = "$PSScriptRoot/../deploy-hostinger.zip"

if (-not (Test-Path "$distBrowser/index.html")) {
    Write-Host "ERRO CRÍTICO: index.html não foi encontrado em $distBrowser!" -ForegroundColor Red
    exit 1
}

Write-Host "===> 2. Configurando roteamento SPA (.htaccess completo com CORS, cache e suporte a vídeo...)" -ForegroundColor Cyan
Copy-Item "$distBrowser/index.html" "$distBrowser/404.html" -Force

# WB AGENCY — .htaccess PRODUÇÃO OTIMIZADO PARA HOSTINGER APACHE (UTF-8 sem BOM)
$htaccessContent = @"
DirectoryIndex index.html index.php

# ===== 1. FORÇAR HTTPS SEMPRE =====
<IfModule mod_rewrite.c>
  RewriteEngine On
  RewriteCond %{HTTPS} off
  RewriteRule ^(.*)$ https://%{HTTP_HOST}%{REQUEST_URI} [L,R=301]
</IfModule>

# ===== 2. CORS PERMISSIVO + HEADERS DE SEGURANÇA =====
<IfModule mod_headers.c>
  Header always set Access-Control-Allow-Origin "*"
  Header always set Access-Control-Allow-Methods "GET, POST, PUT, PATCH, DELETE, OPTIONS"
  Header always set Access-Control-Allow-Headers "Authorization, Content-Type, X-Requested-With, Accept, Origin, Range"
  Header always set Access-Control-Expose-Headers "Accept-Ranges, Content-Length, Content-Range"
  Header always set X-Content-Type-Options "nosniff"
  Header always set X-Frame-Options "SAMEORIGIN"
  Header always set Referrer-Policy "strict-origin-when-cross-origin"

  # Cache: imagens (1 semana imutável)
  <FilesMatch "\.(jpg|jpeg|png|gif|webp|svg|ico|avif)$">
    Header set Cache-Control "public, max-age=604800, immutable"
    Header unset Set-Cookie
  </FilesMatch>

  # Cache: vídeos (1 semana) + suporte byte-range para Safari
  <FilesMatch "\.(mp4|webm|ogv|mov)$">
    Header set Cache-Control "public, max-age=604800"
    Header set Accept-Ranges "bytes"
    Header unset Set-Cookie
  </FilesMatch>

  # Cache: fontes (1 ano imutável)
  <FilesMatch "\.(woff|woff2|ttf|eot|otf)$">
    Header set Cache-Control "public, max-age=31536000, immutable"
    Header unset Set-Cookie
  </FilesMatch>

  # Cache: CSS / JS (30 dias imutável - Angular tem hash no nome)
  <FilesMatch "\.(css|js)$">
    Header set Cache-Control "public, max-age=2592000, immutable"
    Header unset Set-Cookie
  </FilesMatch>

  # Cache: HTML / JSON curto para evitar cache agressivo
  <FilesMatch "\.(html|json)$">
    Header set Cache-Control "public, max-age=60, must-revalidate, no-transform"
    Header unset Set-Cookie
  </FilesMatch>
</IfModule>

# ===== 3. TIPOS MIME OBRIGATÓRIOS =====
<IfModule mod_mime.c>
  AddType image/webp .webp
  AddType image/jpeg .jpg .jpeg
  AddType image/png .png
  AddType image/avif .avif
  AddType video/mp4 .mp4
  AddType video/webm .webm
  AddType application/json .json
  AddType font/woff2 .woff2
  AddType font/woff .woff
</IfModule>

# ===== 4. COMPRESSÃO GZIP =====
<IfModule mod_deflate.c>
  AddOutputFilterByType DEFLATE text/plain text/html text/xml text/css text/javascript application/javascript application/json application/xml image/svg+xml
</IfModule>

# ===== 5. OPÇÕES DE DIRETÓRIO =====
Options -Indexes +FollowSymLinks

# ===== 6. ROTAS SPA ANGULAR — NÃO CAI EM 404 EM ROTAS VIRTUAIS =====
<IfModule mod_rewrite.c>
  RewriteEngine On
  RewriteBase /
  # Arquivo ou diretório físico existe: serve direto
  RewriteCond %{REQUEST_FILENAME} -f [OR]
  RewriteCond %{REQUEST_FILENAME} -d
  RewriteRule ^ - [L]
  # Exclui arquivos com extensões de fallback
  RewriteCond %{REQUEST_URI} !\.(jpg|jpeg|png|gif|webp|svg|mp4|webm|mov|woff|woff2|css|js|json|pdf|zip)$
  # Redireciona todas as rotas virtuais para o index.html
  RewriteRule ^ index.html [L]
</IfModule>
"@

$htPath = "$distBrowser/.htaccess"
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($htPath, ($htaccessContent -replace "`r`n", "`n"), $utf8NoBom)

Write-Host "===> 3. Atualizando pasta deploy-wbagency limpa..." -ForegroundColor Cyan
if (Test-Path $deployDir) {
    Remove-Item "$deployDir/*" -Recurse -Force -ErrorAction SilentlyContinue
} else {
    New-Item -ItemType Directory -Path $deployDir -Force | Out-Null
}
Copy-Item "$distBrowser/*" "$deployDir/" -Recurse -Force
Copy-Item $htPath "$deployDir/.htaccess" -Force

Write-Host "===> 4. Gerando pacote deploy-hostinger.zip com separadores Unix '/'..." -ForegroundColor Cyan
if (Test-Path $zipPath) {
    Remove-Item $zipPath -Force
}

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

$distResolved = (Resolve-Path $distBrowser).Path
$fs = [System.IO.File]::Open($zipPath, 'Create')
$arc = New-Object System.IO.Compression.ZipArchive($fs, [System.IO.Compression.ZipArchiveMode]::Create)

Get-ChildItem $distResolved -Recurse -File -Force | ForEach-Object {
    $rel = $_.FullName.Substring($distResolved.Length + 1).Replace('\', '/')
    [void][System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($arc, $_.FullName, $rel, [System.IO.Compression.CompressionLevel]::Optimal)
}

$arc.Dispose()
$fs.Dispose()

$zipSizeMb = [math]::Round((Get-Item $zipPath).Length / 1MB, 2)

Write-Host "==========================================================================" -ForegroundColor Green
Write-Host "✅ SUCESSO! Pacote deploy-hostinger.zip ($zipSizeMb MB) gerado na raiz do projeto." -ForegroundColor Green
Write-Host "   - 100% compatível com Linux/Apache da Hostinger (separadores Unix /)" -ForegroundColor Gray
Write-Host "   - Contém index.html, .htaccess e assets na raiz" -ForegroundColor Gray
Write-Host "   - Sem nenhuma pasta ou código-fonte do repositório" -ForegroundColor Gray
Write-Host "==========================================================================" -ForegroundColor Green
Write-Host "COMO APLICAR NA HOSTINGER:" -ForegroundColor Yellow
Write-Host "1. No hPanel da Hostinger, abra o 'Gerenciador de Arquivos'." -ForegroundColor Yellow
Write-Host "2. Acesse a pasta 'public_html'." -ForegroundColor Yellow
Write-Host "3. Se houver pastas do repositório (frontend, backend, Projeto, etc.), delete-as." -ForegroundColor Yellow
Write-Host "4. Faça upload do arquivo 'deploy-hostinger.zip' e clique com botão direito: 'Extrair'." -ForegroundColor Yellow
Write-Host "==========================================================================" -ForegroundColor Green

Set-Location -Path "$PSScriptRoot/.."
