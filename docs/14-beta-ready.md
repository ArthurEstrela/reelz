# Pacote beta ready

Este documento é o gate operacional para abrir o CineGiro a pessoas externas. Código pronto não substitui as credenciais e decisões de infraestrutura abaixo.

## O que já está implementado

- confirmação de e-mail com token de uso único, expiração e hash SHA-256 no banco;
- recuperação de senha com resposta neutra, cooldown e invalidação dos links anteriores;
- consulta e alteração do perfil autenticado;
- exclusão de conta com confirmação de senha, anonimização e invalidação imediata do JWT;
- sessão expirada comunicada no front-end;
- rate limit no proxy para login, cadastro e solicitação de recuperação;
- configuração `prod` que exige SMTP e URL pública HTTPS;
- migrations validadas contra PostgreSQL real no CI;
- health/readiness, métricas Prometheus e regras de alerta;
- backup PostgreSQL em formato custom, verificado por `pg_restore --list`;
- atribuição ao TMDB e ao JustWatch nas superfícies de produto.

## Variáveis obrigatórias do ambiente público

Use `.env.example` como contrato, sem versionar o arquivo real. Para produção:

```dotenv
SPRING_PROFILES_ACTIVE=prod
PUBLIC_APP_URL=https://beta.seudominio.com
ACCOUNT_MAIL_MODE=SMTP
MAIL_HOST=smtp.seuprovedor.com
MAIL_PORT=587
MAIL_USERNAME=...
MAIL_PASSWORD=...
MAIL_FROM=no-reply@seudominio.com
MAIL_SMTP_AUTH=true
MAIL_STARTTLS=true
POSTGRES_PASSWORD=...
JWT_SECRET=...
TMDB_READ_ACCESS_TOKEN=...
```

Configure SPF, DKIM e DMARC no domínio de envio. Não use `ACCOUNT_MAIL_MODE=LOG` fora do ambiente local: nesse modo o link secreto aparece no log para facilitar testes.

## Backup e restauração

Criar e validar um backup no volume `reelz_postgres_backups`:

```bash
docker compose --profile operations run --rm db-backup
```

Agende esse comando diariamente no provedor e copie os arquivos para armazenamento externo criptografado. Um volume no mesmo servidor não protege contra perda da máquina.

Antes de abrir o beta, execute ao menos um restore em banco descartável:

```bash
docker compose cp db-backup:/backups/reelz-AAAAMMDDTHHMMSSZ.dump ./restore-test.dump
docker compose exec -T db createdb -U reelz reelz_restore_test
docker compose exec -T db pg_restore -U reelz -d reelz_restore_test --no-owner --no-acl < restore-test.dump
```

Depois valide as tabelas e remova somente o banco descartável. A retenção local padrão é 14 dias (`BACKUP_RETENTION_DAYS`).

## Alertas

Inicie a coleta local com:

```bash
docker compose --profile observability up -d prometheus
```

As regras em `observability/alerts.yml` cobrem indisponibilidade, erros 5xx, latência da roleta e pool de conexões. Em hospedagem pública, conecte essas regras ao canal de plantão do provedor (e-mail, Slack ou PagerDuty). Regra sem destinatário não acorda ninguém.

## Checklist de abertura

- [ ] domínio e TLS válidos;
- [ ] SMTP real testado em Gmail e Outlook, incluindo spam;
- [ ] token TMDB regenerado e guardado apenas no cofre de secrets;
- [ ] termos comerciais do TMDB confirmados para o formato do beta;
- [ ] cadastro, confirmação, reset e exclusão testados no domínio público;
- [ ] sincronização TMDB diária habilitada e último run conferido;
- [ ] backup diário externo e restore ensaiado;
- [ ] alertas entregando a uma pessoa responsável;
- [ ] conta ADMIN separada e protegida;
- [ ] teste mobile em Android e iPhone;
- [ ] grupo inicial limitado e canal de feedback definido.

## Go/no-go

O deploy técnico pode ser feito assim que os itens externos acima estiverem configurados. O beta não deve ser aberto com e-mail em modo `LOG`, sem restore ensaiado ou usando uma credencial TMDB que já tenha sido compartilhada fora do cofre de secrets.
