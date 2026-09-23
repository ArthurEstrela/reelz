package com.roletadefilmes.billing.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.roletadefilmes.billing.domain.exception.BillingProviderException;
import com.roletadefilmes.billing.service.PaymentGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Component
public class MercadoPagoGateway implements PaymentGateway {

    private static final Logger LOGGER = LoggerFactory.getLogger(MercadoPagoGateway.class);

    private final RestClient restClient;

    public MercadoPagoGateway(@Qualifier("mercadoPagoRestClient") RestClient mercadoPagoRestClient) {
        this.restClient = mercadoPagoRestClient;
    }

    @Override
    public CheckoutResult createSubscriptionCheckout(CheckoutCommand command) {
        var request = new CreatePreapprovalRequest(
                command.description(),
                command.externalId(),
                command.payerEmail(),
                new AutoRecurring(
                        command.frequencyInMonths(),
                        "months",
                        amount(command.amountCents()),
                        "BRL"
                ),
                command.returnUrl(),
                "pending"
        );
        try {
            var response = restClient.post()
                    .uri("/preapproval")
                    .body(request)
                    .retrieve()
                    .body(PreapprovalResponse.class);
            if (response == null || !StringUtils.hasText(response.id())
                    || !StringUtils.hasText(response.initPoint()) || response.autoRecurring() == null) {
                throw new BillingProviderException("O Mercado Pago retornou um checkout inválido.");
            }
            return new CheckoutResult(
                    response.id(),
                    response.id(),
                    response.initPoint(),
                    cents(response.autoRecurring().transactionAmount())
            );
        } catch (BillingProviderException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw providerFailure("abrir o checkout", exception);
        }
    }

    @Override
    public void cancelSubscription(String providerSubscriptionId) {
        try {
            restClient.put()
                    .uri("/preapproval/{id}", providerSubscriptionId)
                    .body(Map.of("status", "canceled"))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw providerFailure("cancelar a assinatura", exception);
        }
    }

    @Override
    public ProviderSubscription getSubscription(String providerSubscriptionId) {
        try {
            var response = restClient.get()
                    .uri("/preapproval/{id}", providerSubscriptionId)
                    .retrieve()
                    .body(PreapprovalResponse.class);
            if (response == null || !StringUtils.hasText(response.id())) {
                throw new BillingProviderException("O Mercado Pago retornou uma assinatura inválida.");
            }
            return new ProviderSubscription(
                    response.id(),
                    response.externalReference(),
                    response.status(),
                    response.paymentMethodId()
            );
        } catch (BillingProviderException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw providerFailure("consultar a assinatura", exception);
        }
    }

    @Override
    public ProviderPayment getAuthorizedPayment(String providerPaymentId) {
        try {
            var response = restClient.get()
                    .uri("/authorized_payments/{id}", providerPaymentId)
                    .retrieve()
                    .body(AuthorizedPaymentResponse.class);
            if (response == null || response.payment() == null || response.payment().id() == null) {
                throw new BillingProviderException("O Mercado Pago retornou uma fatura inválida.");
            }
            return new ProviderPayment(
                    response.payment().id().toString(),
                    response.preapprovalId(),
                    response.externalReference(),
                    response.payment().status(),
                    response.currencyId(),
                    cents(response.transactionAmount()),
                    null
            );
        } catch (BillingProviderException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw providerFailure("consultar a fatura", exception);
        }
    }

    @Override
    public ProviderPayment getPayment(String providerPaymentId) {
        try {
            var response = restClient.get()
                    .uri("/v1/payments/{id}", providerPaymentId)
                    .retrieve()
                    .body(PaymentResponse.class);
            if (response == null || response.id() == null) {
                throw new BillingProviderException("O Mercado Pago retornou um pagamento inválido.");
            }
            return new ProviderPayment(
                    response.id().toString(),
                    response.preapprovalId(),
                    response.externalReference(),
                    response.status(),
                    response.currencyId(),
                    cents(response.transactionAmount()),
                    response.paymentMethodId()
            );
        } catch (BillingProviderException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw providerFailure("consultar o pagamento", exception);
        }
    }

    private BillingProviderException providerFailure(String operation, RestClientException exception) {
        if (exception instanceof RestClientResponseException responseException) {
            LOGGER.warn("Mercado Pago rejected operation {} with HTTP {}", operation, responseException.getStatusCode());
        } else {
            LOGGER.warn("Mercado Pago communication failed while trying to {}", operation);
        }
        return new BillingProviderException("Não foi possível " + operation + " no Mercado Pago.", exception);
    }

    private BigDecimal amount(int cents) {
        return BigDecimal.valueOf(cents, 2);
    }

    private int cents(BigDecimal amount) {
        if (amount == null) {
            throw new BillingProviderException("O Mercado Pago retornou um pagamento sem valor.");
        }
        try {
            return amount.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).intValueExact();
        } catch (ArithmeticException exception) {
            throw new BillingProviderException("O Mercado Pago retornou um valor monetário inválido.", exception);
        }
    }

    private record CreatePreapprovalRequest(
            String reason,
            @JsonProperty("external_reference") String externalReference,
            @JsonProperty("payer_email") String payerEmail,
            @JsonProperty("auto_recurring") AutoRecurring autoRecurring,
            @JsonProperty("back_url") String backUrl,
            String status
    ) {
    }

    private record AutoRecurring(
            int frequency,
            @JsonProperty("frequency_type") String frequencyType,
            @JsonProperty("transaction_amount") BigDecimal transactionAmount,
            @JsonProperty("currency_id") String currencyId
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PreapprovalResponse(
            String id,
            @JsonProperty("external_reference") String externalReference,
            String status,
            @JsonProperty("init_point") String initPoint,
            @JsonProperty("payment_method_id") String paymentMethodId,
            @JsonProperty("auto_recurring") AutoRecurring autoRecurring
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AuthorizedPaymentResponse(
            Long id,
            @JsonProperty("preapproval_id") String preapprovalId,
            @JsonProperty("external_reference") String externalReference,
            @JsonProperty("currency_id") String currencyId,
            @JsonProperty("transaction_amount") BigDecimal transactionAmount,
            AuthorizedPaymentDetail payment
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AuthorizedPaymentDetail(Long id, String status) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PaymentResponse(
            Long id,
            @JsonProperty("preapproval_id") String preapprovalId,
            @JsonProperty("external_reference") String externalReference,
            String status,
            @JsonProperty("currency_id") String currencyId,
            @JsonProperty("transaction_amount") BigDecimal transactionAmount,
            @JsonProperty("payment_method_id") String paymentMethodId
    ) {
    }
}
