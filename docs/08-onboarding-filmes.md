# Onboarding de filmes

O onboarding reduz recomendações óbvias antes do primeiro giro. O backend seleciona
25 filmes populares por padrão, com pôster e oferta ativa no país do usuário, e
exclui títulos que já estejam marcados como assistidos.

## Contratos

### Listar filmes

`GET /api/v1/onboarding/movies?limit=25`

O limite aceito é de 20 a 30 filmes. A resposta contém `movieId` (identificador
TMDB), título, caminho do pôster e nota do TMDB.

### Concluir

`POST /api/v1/onboarding/complete`

```json
{
  "presentedMovieIds": [550, 603],
  "watchedMovieIds": [550]
}
```

A lista de assistidos deve ser um subconjunto da lista apresentada. A operação:

1. bloqueia a conta do usuário durante a atualização;
2. consulta históricos existentes em lote;
3. cria ou atualiza somente os títulos marcados como `WATCHED`;
4. grava `onboarding_completed_at` no mesmo commit.

Reenvios não duplicam históricos já assistidos. A migration
`V6__backfill_existing_user_onboarding.sql` marca contas anteriores à entrega
como concluídas, preservando o acesso de usuários já ativos.

## Front-end

A rota privada `/onboarding` apresenta uma pilha de pôsteres com Framer Motion:

- esquerda ou botão “Não assisti” avança sem gravar histórico;
- direita ou botão “Já assisti” adiciona o filme à seleção;
- a conclusão atualiza a sessão local e libera `/` e `/library`;
- usuários incompletos são redirecionados ao onboarding pelas guardas de rota.
