# Reelz

Reelz é uma roleta de filmes gamificada para reduzir o tempo de escolha do que assistir. O produto cruza preferências do usuário, histórico de filmes e disponibilidade em serviços de streaming.

## Estado atual

O repositório está na primeira etapa do backend:

- modelagem inicial do domínio e do PostgreSQL;
- DTOs de cadastro de usuário;
- DTOs de histórico e watchlist;
- validações de entrada;
- testes unitários dos contratos.

O frontend ainda não foi iniciado.

## Stack

- Java 21
- Spring Boot 3.5
- Maven
- PostgreSQL planejado para a próxima etapa

## Executar os testes

```bash
mvn test
```

## Documentação

A modelagem inicial, relacionamentos e decisões de domínio estão em [`docs/01-modelagem-inicial.md`](docs/01-modelagem-inicial.md).
