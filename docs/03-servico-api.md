# Etapa 3 — serviço e API da roleta

## Endpoint

```http
POST /api/v1/roulette/spin
X-User-Id: 2c09cc0a-0de5-41a9-96ae-7035fe8962c3
Content-Type: application/json
```

```json
{
  "idempotencyKey": "eb00bf86-bb7f-4652-82b3-16a076ced021",
  "providerIds": ["c908fc1b-8038-4c78-ab08-b578ce0c92d2"],
  "genreId": 35,
  "vibeId": null
}
```

`X-User-Id` é temporário e simula a identidade autenticada. Ele deve ser substituído pelo usuário extraído do `Principal`/token quando Spring Security for implementado.

## Transação

O fluxo de `RouletteService.spin` é atômico:

1. carrega usuário e uso do dia no fuso do usuário;
2. cria a linha diária com `ON CONFLICT DO NOTHING`, se necessário;
3. reconhece replay pela chave de idempotência;
4. valida plano, provedores e saldo;
5. consulta um filme aleatório;
6. consome um giro somente quando há filme;
7. persiste o uso versionado e a auditoria de sucesso;
8. confirma tudo no mesmo commit.

No banco é armazenada a quantidade usada. Por isso, “consumir/decrementar a franquia” significa incrementar `base_spins_used` ou `rewarded_spins_used`.

## Política de franquia

- Free: cinco giros-base por dia.
- Free com recompensa: depois dos cinco, consome os giros premiados concedidos.
- Premium: ilimitado e não consome a franquia Free; os giros ficam contabilizados em `roulette_spin` para analytics.
- Resultado vazio: retorna erro e não consome giro.
- Replay da mesma chave idempotente: retorna o resultado confirmado sem novo consumo.

## Respostas de erro

| Código | HTTP | Uso |
|---|---:|---|
| `NO_MOVIES_FOUND` | 404 | não há candidato para os filtros |
| `DAILY_SPIN_LIMIT_EXCEEDED` | 429 | precisa aguardar, assistir anúncio ou fazer upgrade |
| `INVALID_PROVIDER_SELECTION` | 400 | seleção vazia ou múltiplos provedores no free |
| `USER_NOT_FOUND` | 404 | usuário inexistente |
| `DUPLICATE_SPIN` | 409 | chave já usada com filtros diferentes ou em giro não concluído com sucesso |
| `CONCURRENT_SPIN_CONFLICT` | 409 | conflito de optimistic locking |
| `VALIDATION_ERROR` | 400 | Bean Validation falhou |

## Decisões pendentes

- Autenticação real e remoção do header simulado.
- Política de retries automáticos após conflito otimista.
- Auditoria durável de tentativas sem candidato. Como `NoMoviesFoundException` causa rollback, apenas giros bem-sucedidos são persistidos nesta etapa.
- Definição de um limite de recompensas por anúncio por dia.
