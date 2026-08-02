# CI/CD e observabilidade

## Pipeline de qualidade

O workflow `.github/workflows/ci.yml` roda em pushes e pull requests para `main`, manualmente e toda segunda-feira. Ele executa:

- `mvn verify`, incluindo os testes de integração com PostgreSQL/Testcontainers;
- `npm ci`, `npm audit`, ESLint, Vitest e o build de produção;
- build reproduzível das imagens Docker do backend e frontend;
- varredura das duas imagens com Grype, bloqueando vulnerabilidades altas ou críticas que possuam correção;
- revisão de novas dependências vulneráveis em pull requests;
- retenção por sete dias do JAR e do `dist` gerados.

As Actions estão fixadas por SHA para reduzir risco de supply chain. O Dependabot verifica semanalmente Maven, npm, Docker e as próprias Actions.

O workflow `.github/workflows/codeql.yml` faz análise estática de Java e TypeScript. O GitHub publica os achados na aba **Security** do repositório.

## Entrega de imagens

O workflow `.github/workflows/release.yml` publica imagens com SBOM e provenance no GitHub Container Registry:

- `ghcr.io/arthurestrela/reelz-backend`
- `ghcr.io/arthurestrela/reelz-frontend`

Uma tag SemVer dispara a publicação:

```bash
git tag v0.1.0
git push origin v0.1.0
```

Isso entrega artefatos de produção versionados. O deploy em um servidor/plataforma será o próximo adaptador e dependerá da hospedagem escolhida.

## Endpoints operacionais

| Endpoint | Finalidade |
| --- | --- |
| `/actuator/health` | Estado geral da aplicação |
| `/actuator/health/liveness` | Indica se o processo deve ser reiniciado |
| `/actuator/health/readiness` | Indica se pode receber tráfego |
| `/actuator/prometheus` | Métricas no formato Prometheus |

O Docker usa readiness em seu `HEALTHCHECK`. O Nginx encaminha somente `/api`, e o Compose publica o backend apenas em `127.0.0.1`; portanto, `/actuator` não deve ser adicionado ao ingress público em produção.

O perfil opcional `observability` inicia um Prometheus local, também restrito a `127.0.0.1`, com retenção de 15 dias:

```bash
docker compose --profile observability up -d
```

A interface fica em `http://localhost:9090`. Em um deploy real, esse serviço deve permanecer em rede privada e receber autenticação no acesso administrativo.

## Métricas do produto

| Métrica Micrometer | Leitura de produto |
| --- | --- |
| `reelz.users.registered` | Cadastros concluídos |
| `reelz.onboarding.completed` | Onboardings concluídos |
| `reelz.onboarding.watched.movies` | Distribuição de filmes marcados durante o onboarding |
| `reelz.roulette.spins` | Giros por resultado e plano |
| `reelz.roulette.spin.duration` | Latência do giro por resultado e plano |

Os resultados de giro são limitados a valores conhecidos (`success`, `replayed`, `no_movies`, `limit_exceeded`, `invalid_request` e `error`). As tags de plano são `free`, `premium` ou `unknown`. IDs de usuário, e-mails, IDs de filme e filtros não entram nas métricas, evitando dados pessoais e cardinalidade sem limite.

Métricas de sucesso transacional são incrementadas apenas após o commit do banco. Assim, uma falha de optimistic locking não aparece falsamente como conversão.

## Correlação de erros

Toda resposta inclui `X-Request-Id`. Se o cliente enviar um identificador seguro nesse header ele é preservado; caso contrário, o backend gera um UUID. O mesmo valor entra no MDC e no padrão dos logs, permitindo localizar uma requisição sem registrar dados do usuário.
