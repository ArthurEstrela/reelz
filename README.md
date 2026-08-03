# Reelz

Reelz é uma roleta de filmes gamificada para reduzir o tempo de escolha do que assistir. O produto cruza preferências do usuário, histórico de filmes e disponibilidade em serviços de streaming.

## Estado atual

O MVP inclui:

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
- rotas públicas e privadas com React Router v8;
- testes de contexto, interceptors e navegação.
- Home da roleta com filtros rápidos, animações de mola e slot machine;
- integração do giro com estados de sucesso, catálogo vazio e limite diário;
- card de resultado responsivo com link para o streaming;
- testes dos fluxos centrais da experiência da roleta.
- endpoint autenticado para registrar filmes assistidos e watchlist;
- sincronização da franquia diária no fuso do usuário;
- catálogos autenticados de provedores e vibes;
- testes end-to-end do backend com JWT, MockMvc e PostgreSQL Testcontainers.
- catálogo dinâmico integrado aos filtros da Home;
- franquia diária sincronizada na abertura e depois de cada giro;
- ação “Já vi” integrada ao histórico, com fallback por toast;
- listagem paginada e enriquecida dos filmes assistidos;
- Biblioteca mobile-first com contador de coleção e carregamento incremental;
- navegação inferior entre Roleta e Biblioteca;
- consistência read-after-write entre “Já vi” e o próximo giro.
- sincronização incremental por provedor, com checkpoint, lease e auditoria operacional;
- catálogo brasileiro de filmes e disponibilidade sincronizado pelo TMDB;
- preferências persistentes de streaming, separadas dos filtros de cada giro;
- atribuição de disponibilidade JustWatch e catálogo filtrado por ofertas elegíveis;
- onboarding Tinder-style com filmes populares e disponíveis no país do usuário;
- histórico inicial persistido em lote e bloqueio de rotas até a conclusão.
- Watchlist integrada ao resultado da roleta e à Biblioteca;
- atualização otimista para remover ou marcar itens da Watchlist como assistidos.
- PWA instalável com manifest, ícones, atalhos e shell offline;
- atualização segura do app sem armazenar respostas autenticadas no cache.
- CI com testes Java/Testcontainers, lint, testes e build do front-end;
- build e auditoria das imagens Docker, `npm audit` e revisão de dependências;
- CodeQL para Java e TypeScript e atualizações semanais via Dependabot;
- publicação versionada das imagens no GHCR por tags `v*.*.*`;
- health/readiness, métricas Prometheus do funil e correlação por `X-Request-Id`.
- analytics de produto pseudonimizados, dashboard administrativo por role e experimentos de interesse para casal/grupo;
- páginas públicas de Termos, Privacidade e atribuição ao TMDB;
- retenção automática de eventos e proteção de login/cadastro por rate limit no Nginx.
- feedback qualitativo dentro do app, agregado no cockpit administrativo, e tela de recuperação para erros fatais do front-end.

A Home privada contém a experiência funcional da roleta e consome os catálogos, a franquia diária e o histórico diretamente dos endpoints autenticados do backend.

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
- React Router
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

## Observabilidade

- Health: `GET http://localhost:8080/actuator/health`
- Readiness: `GET http://localhost:8080/actuator/health/readiness`
- Prometheus: `GET http://localhost:8080/actuator/prometheus`

Para iniciar também o coletor Prometheus local, com retenção de 15 dias:

```bash
docker compose --profile observability up -d
```

A interface de consultas fica em `http://localhost:9090` e também é vinculada apenas a `127.0.0.1`.

No Docker Compose, a porta do backend fica vinculada somente a `127.0.0.1`. Em produção, o proxy público deve continuar encaminhando apenas `/api`, mantendo `/actuator` restrito à rede operacional.

## Documentação

- [Modelagem inicial](docs/01-modelagem-inicial.md)
- [Camada de persistência](docs/02-persistencia.md)
- [Serviço e API da roleta](docs/03-servico-api.md)
- [Spring Security e JWT](docs/04-seguranca-jwt.md)
- [Endpoints de experiência](docs/05-endpoints-experiencia.md)
- [Catálogo TMDB](docs/06-catalogo-tmdb.md)
- [Preferências de streaming](docs/07-preferencias-streaming.md)
- [Onboarding de filmes](docs/08-onboarding-filmes.md)
- [Watchlist](docs/09-watchlist.md)
- [Progressive Web App](docs/10-pwa.md)
- [CI/CD e observabilidade](docs/11-ci-observabilidade.md)
- [Runbook do beta fechado](docs/12-beta-fechado.md)
- [Front-end React](frontend/README.md)
