# Sistema de conquistas

O Reelz usa conquistas para reforçar comportamentos que entregam valor ao usuário. Não há XP por giro, ranking global ou sequência diária: essas mecânicas incentivariam quantidade e ansiedade, enquanto o produto existe para reduzir o tempo de decisão.

## Contrato

`GET /api/v1/achievements` exige JWT e retorna o catálogo ativo, o progresso persistido e o total desbloqueado. A avaliação é retroativa e idempotente: o serviço mede fontes de verdade existentes e mantém o maior progresso já alcançado.

As fontes são:

- giros concluídos em `roulette_spin`;
- histórico `WATCHED` e `WATCHLIST`;
- gêneros presentes nos filmes assistidos;
- abertura de provedor registrada em `product_event`;
- snapshots imutáveis dos participantes de giros sociais;
- semanas distintas com giro bem-sucedido no fuso do usuário.

Uma trava transacional consultiva por usuário serializa avaliações concorrentes. A constraint única `(user_id, achievement_id)` impede duplicação no banco.

## Conquistas do MVP

| Código | Nome | Critério |
|---|---|---|
| `FIRST_SPIN` | Primeira Sessão | 1 giro bem-sucedido |
| `OPEN_PROVIDER` | Sem Enrolação | 1 abertura de streaming |
| `WATCHED_10` | Arquivo Pessoal | 10 assistidos |
| `WATCHED_50` | Cinéfilo de Carteirinha | 50 assistidos |
| `WATCHED_100` | Acervo Vivo | 100 assistidos |
| `WATCHLIST_5` | Na Reserva | 5 itens guardados |
| `GENRES_5` | Saindo da Bolha | 5 gêneros na coleção |
| `COUPLE_SPIN` | Date Night | 1 giro em casal |
| `GROUP_SPIN_3` | Cineclube | 1 giro em grupo com 3+ pessoas |
| `ACTIVE_WEEKS_4` | Sessão Marcada | giros em 4 semanas distintas |

## Front-end

A rota privada `/achievements` exibe a Sala de Troféus. O `AchievementProvider` estabelece uma linha de base ao autenticar e reavalia depois de ações relevantes. Apenas códigos que mudam de bloqueado para desbloqueado durante a sessão geram celebração; conquistas retroativas não inundam a interface na primeira abertura.
