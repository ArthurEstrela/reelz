# Etapa 7 — endpoints de experiência

Todas as rotas desta etapa exigem `Authorization: Bearer <token>`. O usuário é obtido do
`AuthenticatedUser` injetado por `@AuthenticationPrincipal`; nenhum ID de usuário é aceito no payload.

## Histórico

```http
POST /api/v1/history
Content-Type: application/json
```

```json
{
  "movieId": 550,
  "status": "WATCHED"
}
```

`movieId` é o ID numérico do TMDB. O serviço resolve esse valor para a chave UUID interna de
`movie_cache`. Para `WATCHED`, `watchedAt` é gerado pelo backend e `rating` fica nulo. Para
`WATCHLIST`, ambos ficam nulos para respeitar a constraint do banco. Uma nova chamada para o mesmo
usuário e filme atualiza a linha existente.

## Uso diário

```http
GET /api/v1/roulette/usage/today
```

O contrato é o mesmo `SpinQuotaResponse` retornado por `POST /api/v1/roulette/spin`:

```json
{
  "unlimited": false,
  "dailyLimit": 5,
  "remainingDailySpins": 3,
  "remainingRewardedSpins": 2
}
```

A data é calculada no fuso do usuário. A consulta não cria uma linha de uso quando ela ainda não
existe; nesse caso, Free recebe a franquia completa e Premium recebe `unlimited: true`.

## Catálogo

```http
GET /api/v1/catalog/providers
GET /api/v1/catalog/vibes
```

Somente registros ativos são retornados. Os DTOs são deliberadamente leves:

```json
[
  { "id": "c908fc1b-8038-4c78-ab08-b578ce0c92d2", "name": "Netflix" }
]
```

## Migration V2

`V2__fix_watched_at_current_time_check.sql` troca `CURRENT_TIMESTAMP` por `clock_timestamp()` no
trigger de `watched_at`. No PostgreSQL, `CURRENT_TIMESTAMP` representa o início da transação e podia
rejeitar um instante legítimo criado alguns milissegundos depois pelo backend.
