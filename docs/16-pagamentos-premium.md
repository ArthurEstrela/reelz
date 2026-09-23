# Pagamentos e CineGiro Premium

O módulo `billing` integra o CineGiro às Assinaturas do Mercado Pago por uma porta interna (`PaymentGateway`). O checkout é hospedado pelo Mercado Pago: o CineGiro não recebe nem armazena dados de cartão.

## Contrato HTTP

- `GET /api/v1/billing/plans`: catálogo e disponibilidade dos planos.
- `GET /api/v1/billing/subscription`: estado financeiro da conta autenticada.
- `POST /api/v1/billing/checkout`: cria ou reutiliza o checkout pendente. Corpo: `{"planCode":"PREMIUM_MONTHLY"}`.
- `POST /api/v1/billing/subscription/cancel`: cancela imediatamente a assinatura ativa.
- `POST /api/v1/webhooks/mercadopago`: endpoint público que valida `x-signature` e consulta o recurso diretamente na API do Mercado Pago.

O redirecionamento de sucesso nunca concede Premium. O acesso só é liberado quando o Mercado Pago confirma uma cobrança com status `approved`, moeda `BRL` e valor idêntico ao plano reservado.

## Preparação no Mercado Pago

1. Em **Suas integrações**, crie uma aplicação para o CineGiro e selecione pagamentos online/assinaturas quando solicitado.
2. Comece com as credenciais de teste. Copie o **Access Token** para `MERCADOPAGO_ACCESS_TOKEN`; a Public Key não é usada neste checkout hospedado.
3. Em **Webhooks**, configure a URL de teste:
   `https://SEU_BACKEND/api/v1/webhooks/mercadopago`
4. Ative os eventos de **Pagamentos** e **Planos e assinaturas**. O backend processa os tópicos `payment`, `subscription_preapproval` e `subscription_authorized_payment`.
5. Salve e copie a assinatura secreta do webhook para `MERCADOPAGO_WEBHOOK_SECRET`.
6. Use uma conta compradora de teste diferente da conta vendedora para concluir o checkout.

Variáveis necessárias:

```dotenv
MERCADOPAGO_ENABLED=true
MERCADOPAGO_ACCESS_TOKEN=TEST-...
MERCADOPAGO_WEBHOOK_SECRET=...
MERCADOPAGO_CONNECT_TIMEOUT=PT5S
MERCADOPAGO_READ_TIMEOUT=PT15S
REELZ_PREMIUM_MONTHLY_PRICE_CENTS=1290
REELZ_PREMIUM_ANNUAL_PRICE_CENTS=9990
PUBLIC_APP_URL=https://cinegiro-five.vercel.app
```

Depois de validar o fluxo completo, substitua as credenciais de teste pelas credenciais de produção e configure também a URL de produção no Mercado Pago. Nunca misture Access Token de teste com webhook/conta de produção.

## Fluxo e invariantes de segurança

1. O backend reserva uma assinatura local com um UUID próprio.
2. Cria um `preapproval` pendente no Mercado Pago, usando esse UUID como `external_reference`.
3. O frontend redireciona somente para o `init_point` devolvido pelo provedor.
4. No webhook, a assinatura HMAC é validada usando `data.id`, `x-request-id`, o timestamp e o secret da aplicação.
5. O payload recebido não é considerado fonte da verdade: o backend busca a assinatura, fatura ou pagamento na API do Mercado Pago.
6. Cada pagamento/status é idempotente em `payment_webhook_event`, impedindo dupla renovação em retentativas ou tópicos sobrepostos.
7. Valor e moeda são conferidos antes da ativação ou renovação.
8. O cancelamento no Mercado Pago acontece antes da remoção local do Premium.

A migração V19 preserva registros históricos da AbacatePay, mas cancela checkouts antigos ainda pendentes. Nenhuma nova cobrança usa o provedor anterior.

## Teste local

Com `MERCADOPAGO_ENABLED=false`, a tela `/premium` continua visível, mas os planos ficam indisponíveis para checkout. Para testar pagamentos reais é necessário um backend HTTPS público, pois o Mercado Pago precisa entregar os webhooks.

Ao testar, confira:

- o checkout abre no domínio do Mercado Pago;
- retornar ao CineGiro não ativa o Premium sozinho;
- uma cobrança aprovada ativa o plano;
- reenviar o mesmo webhook não estende o plano novamente;
- cancelar no CineGiro altera o `preapproval` para `canceled` no provedor.
