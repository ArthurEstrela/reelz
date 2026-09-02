# Pagamentos e CineGiro Premium

O módulo `billing` integra o CineGiro à API v2 da AbacatePay por uma porta interna (`PaymentGateway`). O restante do produto não conhece detalhes do provedor, permitindo trocar ou adicionar outro gateway sem reescrever as regras de assinatura.

## Contrato HTTP

- `GET /api/v1/billing/plans`: catálogo e disponibilidade dos planos.
- `GET /api/v1/billing/subscription`: estado financeiro da conta autenticada.
- `POST /api/v1/billing/checkout`: cria ou reutiliza o checkout pendente. Corpo: `{"planCode":"PREMIUM_MONTHLY"}`.
- `POST /api/v1/billing/subscription/cancel`: cancela imediatamente a assinatura ativa.
- `POST /api/v1/webhooks/abacatepay`: endpoint público autenticado pelo secret da URL e HMAC do corpo bruto.

O redirecionamento de sucesso nunca concede Premium. Apenas os eventos `subscription.completed` e `subscription.renewed`, após validação criptográfica e idempotência, atualizam `user_account.plan` e `premium_until`.

## Preparação da AbacatePay

1. Crie no painel/API um produto mensal com `cycle=MONTHLY`, preço de 1290 centavos.
2. Crie um produto anual com `cycle=ANNUALLY`, preço de 9990 centavos.
3. Copie apenas os IDs públicos `prod_...` para as variáveis de ambiente.
4. Gere um secret aleatório para a URL: `openssl rand -hex 32`.
5. Cadastre o webhook HTTPS: `https://SEU_DOMINIO/api/v1/webhooks/abacatepay?webhookSecret=SEU_SECRET`.
6. Assine os eventos `subscription.completed`, `subscription.renewed`, `subscription.payment_failed` e `subscription.cancelled`.
7. Configure a chave HMAC pública informada pela documentação/painel. Ela não é a chave privada da API.

Variáveis necessárias:

```dotenv
ABACATEPAY_ENABLED=true
ABACATEPAY_API_KEY=abc_...
ABACATEPAY_WEBHOOK_SECRET=...
ABACATEPAY_WEBHOOK_HMAC_KEY=...
ABACATEPAY_MONTHLY_PRODUCT_ID=prod_...
ABACATEPAY_ANNUAL_PRODUCT_ID=prod_...
ABACATEPAY_METHODS=CARD
ABACATEPAY_ACCEPT_DEV_EVENTS=false
REELZ_PREMIUM_MONTHLY_PRICE_CENTS=1290
REELZ_PREMIUM_ANNUAL_PRICE_CENTS=9990
PUBLIC_APP_URL=https://seu-dominio
```

Para testar eventos de uma conta/chave de desenvolvimento, use `ABACATEPAY_ACCEPT_DEV_EVENTS=true` somente no ambiente de teste. Produção deve permanecer em `false`.

## Invariantes de segurança

- A chave da API existe somente no backend e nunca usa prefixo `VITE_`.
- Secret da URL e assinatura HMAC são comparados em tempo constante.
- O hash SHA-256 do payload é guardado; o payload completo, que pode conter dados pessoais, não é persistido.
- `provider_event_id` é único, impedindo renovação duplicada em retentativas do webhook.
- O valor do checkout e o valor confirmado pelo webhook precisam coincidir com a reserva local.
- Existe no máximo uma assinatura pendente/ativa/inadimplente por usuário.
- A exclusão da conta é bloqueada enquanto houver checkout ou assinatura em andamento, evitando cobranças órfãs.
- O cancelamento no provedor acontece antes da remoção local do Premium.

## Teste local

Com pagamentos desativados, a tela `/premium` continua visível e os botões informam que o checkout estará disponível em breve. Isso permite rodar o projeto e o CI sem credenciais externas.

Para um teste real, exponha o frontend por HTTPS, configure as variáveis acima e use o ambiente de desenvolvimento da AbacatePay. Nunca publique `.env` nem cole a chave privada em issues ou mensagens.
