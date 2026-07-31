# Catálogo TMDB

O catálogo local é sincronizado pelo backend. O token do TMDB nunca é enviado ao React e cada giro
consulta somente o PostgreSQL.

## Configuração

```dotenv
TMDB_READ_ACCESS_TOKEN=
TMDB_LANGUAGE=pt-BR
TMDB_REGION=BR
TMDB_PAGES_PER_PROVIDER=3
TMDB_MAX_PROVIDERS=10
TMDB_PROVIDER_IDS=8,119,350,337,531,1899,307,283,11,300
TMDB_SYNC_ON_STARTUP=false
TMDB_SCHEDULED_SYNC_ENABLED=false
TMDB_SYNC_CRON=0 0 4 * * *
TMDB_SYNC_ZONE=America/Sao_Paulo
TMDB_SYNC_LEASE_DURATION=PT30M
TMDB_MAX_ATTEMPTS=3
TMDB_RETRY_BASE_DELAY=PT0.25S
TMDB_CONNECT_TIMEOUT=PT5S
TMDB_READ_TIMEOUT=PT15S
```

`TMDB_PROVIDER_IDS` define os provedores prioritários do MVP. Se ficar vazio, a sincronização usa os
primeiros provedores retornados pelo TMDB, limitada por `TMDB_MAX_PROVIDERS`. Os IDs padrão cobrem
Netflix, Prime Video, Apple TV, Disney+, Paramount+, Max, Globoplay, Crunchyroll, MUBI e Pluto TV.

Cada página do `Discover` possui até 20 filmes. Em uma primeira carga com dez provedores e três páginas
por provedor, serão avaliadas até 600 posições antes da deduplicação. Aumente a profundidade aos poucos;
o limite interno é de 50 páginas por provedor e a página máxima aceita pelo TMDB é 500.

## Fluxo v2

1. Consulta os provedores oficiais disponíveis na região configurada.
2. Executa o `Discover` separadamente para cada provedor com `watch_region`,
   `with_watch_providers` e os tipos `flatrate|free|ads`.
3. Deduplica filmes que aparecem em mais de um provedor durante a mesma execução.
4. Consulta a disponibilidade do filme e atualiza filme, provedores e ofertas por chave natural.
5. Persiste um checkpoint por provedor. A próxima execução continua da próxima página e volta à
   primeira ao atingir o fim.
6. Se uma página falhar, o checkpoint não avança; ela será tentada novamente na execução seguinte.
7. Mantém todas as ofertas no cache, mas ativa para os filtros somente os provedores curados em
   `TMDB_PROVIDER_IDS`.

As requisições que recebem HTTP 429 ou erro 5xx são repetidas com backoff curto e limitado. Um lease no
PostgreSQL impede duas instâncias do backend de sincronizarem o mesmo catálogo ao mesmo tempo.

## Operação

- `catalog_sync_run` registra status, duração implícita e contadores de cada execução.
- `catalog_sync_checkpoint` registra a próxima página e o último erro de cada provedor.
- `catalog_sync_lease` coordena múltiplas instâncias da aplicação.
- Os status possíveis são `SUCCEEDED`, `PARTIAL`, `FAILED` e `SKIPPED`.

Para desenvolvimento local, `TMDB_SYNC_ON_STARTUP=true` é conveniente. Em produção, mantenha essa
opção desligada e habilite `TMDB_SCHEDULED_SYNC_ENABLED=true`, com o cron em um horário de baixo uso.

## Disponibilidade e atribuição

O catálogo exposto ao usuário lista somente provedores que possuem ofertas `FLATRATE`, `FREE` ou
`ADS` no país. A disponibilidade é fornecida pelo JustWatch via TMDB; o frontend deve manter a
atribuição e abrir a página informada pelo TMDB, sem prometer um deep link direto no streaming.

## Segurança

- Use o Read Access Token somente no backend.
- Nunca use o prefixo `VITE_` para esse token.
- Não versione o arquivo `.env`.
- Em produção, use um gerenciador de segredos.
- Respeite respostas HTTP 429 e os termos comerciais antes de publicar ou monetizar o produto.
