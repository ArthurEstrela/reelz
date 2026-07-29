# Reelz

Reelz é uma roleta de filmes gamificada para reduzir o tempo de escolha do que assistir. O produto cruza preferências do usuário, histórico de filmes e disponibilidade em serviços de streaming.

## Estado atual

O repositório está na terceira etapa do backend:

- modelagem inicial do domínio e do PostgreSQL;
- DTOs de cadastro de usuário;
- DTOs de histórico e watchlist;
- validações de entrada;
- testes unitários dos contratos;
- schema PostgreSQL versionado com Flyway;
- entidades e repositories Spring Data JPA;
- query aleatória do motor da roleta;
- testes de integração com PostgreSQL Testcontainers;
- serviço transacional do motor da roleta;
- endpoint REST para executar giros;
- erros de negócio padronizados e testes unitários com Mockito.

O frontend ainda não foi iniciado.

## Stack

- Java 21
- Spring Boot 3.5
- Maven
- PostgreSQL 16
- Flyway
- Testcontainers

## Executar os testes

```bash
mvn test
```

Os testes de persistência precisam do Docker em execução. Para iniciar a aplicação, configure `DATABASE_URL`, `DATABASE_USERNAME` e `DATABASE_PASSWORD`; os valores padrão apontam para um PostgreSQL local chamado `reelz`.

## Documentação

- [Modelagem inicial](docs/01-modelagem-inicial.md)
- [Camada de persistência](docs/02-persistencia.md)
- [Serviço e API da roleta](docs/03-servico-api.md)
