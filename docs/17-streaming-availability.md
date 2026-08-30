# Streaming Availability API

O adaptador da **Streaming Availability API by Movie of the Night** sincroniza o catálogo brasileiro
em background. A roleta continua consultando exclusivamente o PostgreSQL; nenhum giro depende da
latência ou da disponibilidade da API externa.

## Estratégia de sincronização

1. O backend consulta `/countries/br` para reconciliar os provedores configurados.
2. Enquanto o bootstrap não termina, `/shows/search/filters` importa páginas de 20 filmes ordenadas
   por popularidade. O cursor é persistido em `catalog_sync_checkpoint` e a execução seguinte retoma
   exatamente do ponto salvo.
3. Depois do bootstrap, `/changes` busca somente filmes atualizados dentro de uma janela fechada. O
   cursor e os limites da janela também são persistidos, evitando lacunas em reinícios ou deploys.
4. Um lease no PostgreSQL impede duas instâncias de executar a sincronização simultaneamente.
5. Cada filme é gravado junto com suas ofertas atuais. Ofertas removidas na fonte são removidas do
   catálogo local para aquele conjunto de provedores.

Com os valores padrão, o bootstrap consome até 21 requisições por execução: uma para provedores e
vinte páginas, equivalentes a até 400 filmes. Após o bootstrap, a rotina normalmente consome uma
requisição de provedores e uma ou poucas páginas de alterações.

## Configuração

```dotenv
STREAMING_AVAILABILITY_ENABLED=true
STREAMING_AVAILABILITY_API_KEY=guarde-a-no-cofre-de-secrets
STREAMING_AVAILABILITY_COUNTRY=BR
STREAMING_AVAILABILITY_OUTPUT_LANGUAGE=en
STREAMING_AVAILABILITY_CATALOGS=netflix.subscription,prime.subscription,disney.subscription,hbo.subscription,apple.subscription,paramount.subscription,mubi.subscription,curiosity.subscription,plutotv.free,crunchyroll.subscription
STREAMING_AVAILABILITY_BOOTSTRAP_PAGES_PER_RUN=20
STREAMING_AVAILABILITY_CHANGES_PAGES_PER_RUN=10
STREAMING_AVAILABILITY_SYNC_ON_STARTUP=true
STREAMING_AVAILABILITY_SCHEDULED_SYNC_ENABLED=true
ROULETTE_CATALOG_SOURCE=STREAMING_AVAILABILITY
```

`ROULETTE_CATALOG_SOURCE` controla somente a fonte elegível durante o giro, sem remover dados:

- `STREAMING_AVAILABILITY`: testa exclusivamente as ofertas do Movie of the Night;
- `TMDB`: testa exclusivamente as ofertas legadas do TMDB;
- `ALL`: usa as duas fontes no mesmo catálogo local.

Após trocar o valor no `.env`, recrie o backend para aplicar a configuração:

```powershell
docker compose up -d --force-recreate backend
```

No primeiro deploy, use `SYNC_ON_STARTUP=true` para importar o primeiro lote sem esperar o cron e
mantenha `SCHEDULED_SYNC_ENABLED=true` para que os lotes seguintes continuem diariamente. Depois
do primeiro deploy, desligue `SYNC_ON_STARTUP`; o job agendado concluirá o bootstrap e passará
automaticamente para a sincronização incremental. Confira o progresso em `catalog_sync_run` e
`catalog_sync_checkpoint`.

Não habilite a sincronização agendada do TMDB e da Movie of the Night ao mesmo tempo. Durante o teste
comparativo, mantenha `STREAMING_AVAILABILITY_DEACTIVATE_UNCONFIGURED_PROVIDERS=false`. No corte
definitivo, esse valor pode ser alterado para `true`; isso desativa serviços que a nova fonte não
oferece, inclusive o Globoplay.

## Compatibilidade e limitações

- O ID do TMDB retornado pela nova API continua sendo a chave externa pública do Reelz. Histórico,
  watchlist e URLs existentes não precisam ser migrados.
- A nota de 0 a 100 é normalizada para a escala de 0 a 10 já usada pelo frontend.
- Imagens e logos podem ser URLs absolutas; o frontend aceita tanto essas URLs quanto caminhos antigos
  do TMDB.
- A API ainda não fornece localização em português. O conteúdo importado usa inglês até existir uma
  etapa de tradução/cache em pt-BR.
- O Globoplay não consta no catálogo brasileiro da fonte. Não faça o corte definitivo sem aceitar
  explicitamente essa perda ou contratar uma segunda fonte licenciada.
- A atribuição "Streaming Availability API by Movie of the Night" deve permanecer visível e apontar
  para `https://www.movieofthenight.com/about/api`.

Nunca prefixe a chave com `VITE_`: ela é um segredo exclusivo do backend.
