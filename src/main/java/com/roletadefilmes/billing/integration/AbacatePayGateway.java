package com.roletadefilmes.billing.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.roletadefilmes.billing.domain.exception.BillingProviderException;
import com.roletadefilmes.billing.service.PaymentGateway;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Component
public class AbacatePayGateway implements PaymentGateway {

    private final RestClient restClient;

    public AbacatePayGateway(RestClient abacatePayRestClient) {
        this.restClient = abacatePayRestClient;
    }

    @Override
    public CheckoutResult createSubscriptionCheckout(CheckoutCommand command) {
        var request = new CreateSubscriptionRequest(
                List.of(new CheckoutItem(command.productId(), 1)),
                command.methods(),
                command.returnUrl(),
                command.completionUrl(),
                command.externalId(),
                command.metadata()
        );
        try {
            var response = restClient.post()
                    .uri("/subscriptions/create")
                    .body(request)
                    .retrieve()
                    .body(CreateSubscriptionResponse.class);
            if (response == null || !response.success() || response.data() == null
                    || !StringUtils.hasText(response.data().id()) || !StringUtils.hasText(response.data().url())) {
                throw new BillingProviderException(providerError(response == null ? null : response.error()));
            }
            return new CheckoutResult(response.data().id(), response.data().url(), response.data().amount());
        } catch (BillingProviderException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new BillingProviderException("Não foi possível abrir o checkout de pagamento.", exception);
        }
    }

    @Override
    public void cancelSubscription(String providerSubscriptionId) {
        try {
            var response = restClient.post()
                    .uri("/subscriptions/cancel")
                    .body(Map.of("id", providerSubscriptionId))
                    .retrieve()
                    .body(CancelSubscriptionResponse.class);
            if (response == null || !response.success()) {
                throw new BillingProviderException(providerError(response == null ? null : response.error()));
            }
        } catch (BillingProviderException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new BillingProviderException("Não foi possível cancelar a assinatura no provedor.", exception);
        }
    }

    private String providerError(JsonNode error) {
        if (error != null && !error.isNull() && StringUtils.hasText(error.asText())) {
            return "O provedor de pagamentos recusou a operação: " + error.asText();
        }
        return "O provedor de pagamentos retornou uma resposta inválida.";
    }

    private record CreateSubscriptionRequest(
            List<CheckoutItem> items,
            List<String> methods,
            String returnUrl,
            String completionUrl,
            String externalId,
            Map<String, String> metadata
    ) {
    }

    private record CheckoutItem(String id, int quantity) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CreateSubscriptionResponse(CheckoutData data, JsonNode error, boolean success) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CheckoutData(String id, String url, int amount) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CancelSubscriptionResponse(JsonNode data, JsonNode error, boolean success) {
    }
}
