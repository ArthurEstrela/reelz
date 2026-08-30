# Etapa 2 — persistência

## Estrutura

- A migration Flyway `V1__init_schema.sql` é a fonte de verdade do schema.
- As entidades ficam dentro do módulo funcional correspondente, em `persistence/entity`.
- Os repositories ficam em `persistence/repository`.
- Relações JPA são unidirecionais e `LAZY`; não há coleções bidirecionais nas entidades do MVP.
- IDs internos são UUIDs gerados pelo Hibernate. IDs do TMDB permanecem como chaves externas únicas.

## Motor da roleta

`MovieCacheRepository.findRandomAvailableMovie` recebe:

- usuário;
- provedores selecionados (a lista deve ser não vazia);
- país;
- gênero opcional;
- vibe opcional.

A consulta usa `EXISTS` para disponibilidade e preferência, `NOT EXISTS` para excluir `WATCHED`, e `ORDER BY RANDOM() LIMIT 1`. Não existe peso por nota, popularidade ou outro metadado.

Quando gênero e vibe são enviados juntos, ambos são obrigatórios (`AND`). Uma vibe corresponde a qualquer interseção entre `movie_cache.genre_ids` e `vibe.genre_ids`.

No MVP, ofertas `FLATRATE`, `FREE` e `ADS` são elegíveis. `RENT` e `BUY` ficam fora da roleta de assinaturas.

## Concorrência

`roulette_daily_usage.version` está mapeado com `@Version`. Dois giros concorrentes que carreguem a mesma versão não podem confirmar a atualização: o segundo recebe uma exceção de optimistic locking e o caso de uso deverá refazer a operação ou responder conflito.

O limite de três giros não é uma constraint da tabela. Ele depende do plano e deve ser verificado na mesma transação que incrementa o contador.

## Constraints relevantes

- Nota do usuário entre 1 e 5.
- `WATCHLIST` sem data de visualização ou nota.
- Data de visualização não futura, validada por trigger PostgreSQL.
- Contadores de giro não negativos e giros premiados usados menores ou iguais aos concedidos.
- Idempotência por usuário nos giros e por evento externo nas recompensas.

## Testes

Os testes usam `@DataJpaTest`, Flyway e `postgres:16-alpine` via Testcontainers. Se o Docker não estiver disponível, os testes de integração são reportados como ignorados.

```bash
mvn test
```

O `ddl-auto=validate` garante que a migration e os mapeamentos JPA continuem compatíveis.

## Limite conhecido

`ORDER BY RANDOM()` exige avaliar e ordenar todo o conjunto elegível. É adequado para o volume esperado do MVP e atende à aleatoriedade definida, mas deverá ser medido com `EXPLAIN (ANALYZE, BUFFERS)` quando o cache crescer. Uma mudança futura do mecanismo de amostragem precisa preservar uma distribuição uniforme comprovável.
