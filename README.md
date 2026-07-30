# Reelz

Reelz é uma roleta de filmes gamificada para reduzir o tempo de escolha do que assistir. O produto cruza preferências do usuário, histórico de filmes e disponibilidade em serviços de streaming.

## Estado atual

O repositório está na sexta etapa do produto:

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
- erros de negócio padronizados e testes unitários com Mockito;
- autenticação stateless com Spring Security e JWT;
- cadastro com BCrypt e registro de aceites legais;
- rotas públicas/protegidas e principal tipado na roleta;
- testes MVC da cadeia de segurança;
- front-end React com Vite e TypeScript;
- interface dark, responsiva e mobile-first com Tailwind CSS;
- autenticação JWT integrada via Axios e `sessionStorage`;
- rotas públicas e privadas com React Router DOM v6;
- testes de contexto, interceptors e navegação.
- Home da roleta com filtros rápidos, animações de mola e slot machine;
- integração do giro com estados de sucesso, catálogo vazio e limite diário;
- card de resultado responsivo com link para o streaming;
- testes dos fluxos centrais da experiência da roleta.

A Home privada contém a experiência funcional da roleta. Os catálogos de provedores e vibes são configurados no ambiente enquanto o backend ainda não oferece endpoints públicos para consultá-los.

## Stack

- Java 21
- Spring Boot 3.5
- Maven
- PostgreSQL 16
- Flyway
- Testcontainers
- Spring Security
- JJWT
- React
- TypeScript
- Vite
- Tailwind CSS
- React Router DOM
- Framer Motion

## Executar os testes

```bash
mvn test
```

Os testes de persistência precisam do Docker em execução. Para iniciar a aplicação, configure `DATABASE_URL`, `DATABASE_USERNAME` e `DATABASE_PASSWORD`; os valores padrão apontam para um PostgreSQL local chamado `reelz`.

Também é obrigatório configurar `JWT_SECRET` com uma chave aleatória de pelo menos 256 bits em Base64. Não existe segredo padrão no código.

Para executar o front-end:

```bash
cd frontend
npm install
npm run dev
```

## Documentação

- [Modelagem inicial](docs/01-modelagem-inicial.md)
- [Camada de persistência](docs/02-persistencia.md)
- [Serviço e API da roleta](docs/03-servico-api.md)
- [Spring Security e JWT](docs/04-seguranca-jwt.md)
- [Front-end React](frontend/README.md)
