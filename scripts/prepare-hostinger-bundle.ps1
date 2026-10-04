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

Write-Host "===> 2. Configurando roteamento SPA (.htaccess completo com CORS, cache e suporte a vídeo...)" -ForegroundColor Cyan
Copy-Item "$distBrowser/index.html" "$distBrowser/404.html" -Force

# WB AGENCY — .htaccess PRODUÇÃO OTIMIZADO PARA HOSTINGER APACHE
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
  # Qualquer arquivo físico existe: serve DIRETO (imagens, vídeos, assets
  RewriteCond %{REQUEST_FILENAME} -f [OR]
  RewriteCond %{REQUEST_FILENAME} -d
  RewriteRule ^ - [L]
  # Exclui arquivos com extensões estáticas do fallback (404 elegante)
  RewriteCond %{REQUEST_URI} !\.(jpg|jpeg|png|gif|webp|svg|mp4|webm|mov|woff|woff2|css|js|json|pdf|zip)$
  # Tudo o mais cai no index.html (rotas Angular)
  RewriteRule ^ index.html [L]
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
