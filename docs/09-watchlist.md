# Watchlist

A lista “Quero Ver” reutiliza `user_movie_history` com o status `WATCHLIST`.
Como existe uma restrição única por usuário e filme, um título nunca fica
duplicado entre a watchlist e os assistidos: a alteração de status atualiza o
mesmo registro.

## Endpoints

### Salvar para depois

`POST /api/v1/history`

```json
{
  "movieId": 603,
  "status": "WATCHLIST"
}
```

### Listar

`GET /api/v1/history?status=WATCHLIST&page=0&size=24`

O parâmetro `status` é opcional e mantém `WATCHED` como padrão para preservar o
contrato anterior. A watchlist é ordenada pela atualização mais recente.

### Remover

`DELETE /api/v1/history/watchlist/{movieId}`

O `movieId` é o identificador do TMDB. A remoção é idempotente e só exclui
registros que ainda estejam como `WATCHLIST`; um filme já marcado como assistido
nunca é apagado por esta rota.

## Experiência

- o resultado da roleta pode ser salvo sem fechar o card;
- a Biblioteca possui abas “Assistidos” e “Quero Ver”;
- itens da watchlist podem ser removidos ou convertidos para `WATCHED`;
- as duas ações usam atualização otimista com restauração do card em caso de
  erro;
- a migration V7 adiciona um índice parcial para a paginação da watchlist.
