# Operação do beta fechado

Este documento é o runbook mínimo para colocar pessoas reais no CineGiro sem transformar o teste de produto em um risco operacional.

## Escopo permitido

O beta deve ser fechado, gratuito e de validação. A credencial pessoal do TMDB não autoriza lançar ou monetizar o produto comercialmente. Antes de uma abertura pública/comercial, formalize o plano de uso com o TMDB e revise os termos dos provedores de disponibilidade.

O aplicativo já exibe a atribuição exigida ao TMDB na página **Sobre** e mantém links para Termos e Privacidade no cadastro e na Home.

## Configuração obrigatória

Copie `.env.example` para `.env` e substitua todos os exemplos. O arquivo real não pode ser enviado ao Git.

- `POSTGRES_PASSWORD`: senha exclusiva e aleatória;
- `JWT_SECRET`: pelo menos 32 bytes aleatórios em Base64;
- `ADMIN_EMAILS`: e-mail da conta que acessará `/admin/analytics`;
- `TMDB_READ_ACCESS_TOKEN`: token somente no backend;
- `TMDB_SCHEDULED_SYNC_ENABLED=true`: mantém ofertas atualizadas diariamente;
- `TMDB_SYNC_ON_STARTUP=true`: use no primeiro deploy para popular o catálogo;
- `ANALYTICS_RETENTION_DAYS=180`: retenção padrão dos eventos pseudonimizados.

Registre a conta administrativa com o mesmo e-mail configurado em `ADMIN_EMAILS`. Se ela já existir, reiniciar o backend promove a conta. Faça logout e login novamente para receber um JWT com a role atualizada.

## Subida e verificação

```bash
docker compose build
docker compose up -d
docker compose ps
docker compose logs --tail=100 backend
```

Checklist de fumaça em `http://localhost`:

1. cadastrar uma conta e concluir o onboarding;
2. selecionar um streaming e girar;
3. abrir o link do provedor;
4. salvar em Quero ver e depois marcar como assistido;
5. conferir Biblioteca e contador diário;
6. testar logout/login e instalação como PWA;
7. entrar com a conta admin e abrir `/admin/analytics`;
8. conferir `GET /actuator/health/readiness` apenas na máquina do servidor.

O Nginx limita tentativas nos endpoints públicos de login e cadastro, aplica cabeçalhos de segurança e é o único componente que deve ficar exposto. PostgreSQL e Prometheus não possuem porta pública; o backend fica vinculado a `127.0.0.1` apenas para diagnóstico local.

## O que o painel mede

O painel administrativo trabalha somente com agregados:

- cadastros e ativação do onboarding;
- usuários que executaram o primeiro giro;
- sessões que chegaram ao clique para assistir;
- média de giros até a decisão;
- retenção D7;
- Watchlist e filmes assistidos;
- interesse declarado nos experimentos de modo casal e grupo.
- notas e comentários enviados voluntariamente pelo feedback dentro do aplicativo.

Os eventos de produto são idempotentes e associados a identificadores técnicos de usuário e sessão. Não armazenamos e-mail, IP, texto livre ou dados do dispositivo na tabela de analytics. Eventos individuais expiram automaticamente conforme `ANALYTICS_RETENTION_DAYS`.

Metas sugeridas para os primeiros 30 a 50 testadores:

- ativação do onboarding: pelo menos 70%;
- primeiro giro entre cadastrados: pelo menos 60%;
- decisão por sessão: pelo menos 25%;
- média até decisão: no máximo 3 giros;
- retenção D7: pelo menos 20%.

Essas metas são hipóteses, não compromissos. Combine os números com entrevistas curtas: “Você decidiu mais rápido?”, “Confiou no resultado?” e “O que impediu você de assistir?”.

## Privacidade e suporte

Antes dos convites, defina um e-mail ou canal privado de suporte e inclua-o na mensagem do beta. Atenda pedidos de acesso, correção ou exclusão manualmente durante esta fase. Não conecte AdMob nem publicidade comportamental no beta fechado.

Faça backup diário do volume PostgreSQL no ambiente hospedado e teste uma restauração antes de convidar usuários. Não exponha `/actuator`, Prometheus, banco ou dashboard sem autenticação.

## Critérios para interromper o teste

Pause novos convites se ocorrer qualquer um destes casos:

- credencial ou segredo exposto;
- indisponibilidade persistente ou perda de dados;
- catálogo sem ofertas válidas para a maioria dos filtros;
- links de streaming incorretos em volume relevante;
- regressão que permita repetir filmes já marcados como assistidos.

## Antes do lançamento público

O beta fechado não substitui estas pendências: contrato comercial do TMDB, revisão jurídica dos textos, HTTPS e domínio definitivos, recuperação de senha, verificação de e-mail, exclusão self-service da conta, backup gerenciado e canal público de suporte.
