# Progressive Web App

O frontend do CineGiro pode ser instalado como aplicativo em navegadores
compatíveis. A implementação não adiciona plugins de build e mantém a estratégia
de cache explícita e auditável.

## Componentes

- `manifest.webmanifest`: nome, cores, ícones, modo standalone e atalhos para
  Roleta e Biblioteca;
- ícones PNG em 192, 512 e 512 maskable, além do ícone de 180 para iOS;
- `serviceWorkerTemplate.ts`: fonte do worker;
- plugin local no Vite: gera `dist/sw.js` com uma impressão digital dos bundles;
- `PwaStatusPrompt`: oferece instalação quando o navegador permitir e avisa
  quando uma nova versão estiver pronta.

## Política de cache

- navegações same-origin usam network-first e recorrem ao shell salvo quando a
  rede falha;
- assets versionados, manifest e ícones usam cache-first;
- requisições diferentes de `GET` não são interceptadas;
- qualquer rota `/api/` é ignorada;
- recursos de outras origens, incluindo imagens do TMDB, não são armazenados;
- caches de builds anteriores são removidos na ativação.

Assim, JWT, histórico, quota diária e demais respostas autenticadas nunca são
persistidos pelo service worker.

## Deploy

O Nginx envia `sw.js` com `no-cache, no-store, must-revalidate`. Cada mudança no
bundle altera o conteúdo do worker e permite que o navegador encontre a nova
versão. O usuário pode ativá-la pelo aviso “Nova versão disponível”.

Service workers exigem HTTPS em produção; `localhost` é aceito pelos navegadores
para desenvolvimento.
