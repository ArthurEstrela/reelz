package com.roletadefilmes.billing.integration;

import com.roletadefilmes.billing.service.PaymentGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MercadoPagoGatewayTest {

    private MockRestServiceServer server;
    private MercadoPagoGateway gateway;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl("https://api.mercadopago.com");
        server = MockRestServiceServer.bindTo(builder).build();
        gateway = new MercadoPagoGateway(builder.build());
    }

    @Test
    void shouldCreateAHostedPendingSubscriptionWithServerControlledTerms() {
        server.expect(requestTo("https://api.mercadopago.com/preapproval"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "reason":"CineGiro Premium mensal",
                          "external_reference":"57de82d2-03bb-47c2-99d9-896d24cedc90",
                          "payer_email":"payer@cinegiro.app",
                          "auto_recurring":{
                            "frequency":1,
                            "frequency_type":"months",
                            "transaction_amount":12.90,
                            "currency_id":"BRL"
                          },
                          "back_url":"https://cinegiro.app/premium?checkout=success",
                          "status":"pending"
                        }
                        """, true))
                .andRespond(withSuccess("""
                        {
                          "id":"preapproval_123",
                          "external_reference":"57de82d2-03bb-47c2-99d9-896d24cedc90",
                          "status":"pending",
                          "init_point":"https://www.mercadopago.com.br/subscriptions/checkout?preapproval_id=preapproval_123",
                          "auto_recurring":{
                            "frequency":1,
                            "frequency_type":"months",
                            "transaction_amount":12.90,
                            "currency_id":"BRL"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        var result = gateway.createSubscriptionCheckout(new PaymentGateway.CheckoutCommand(
                "57de82d2-03bb-47c2-99d9-896d24cedc90",
                "payer@cinegiro.app",
                "CineGiro Premium mensal",
                1290,
                1,
                "https://cinegiro.app/premium?checkout=success"
        ));

        assertThat(result.providerSubscriptionId()).isEqualTo("preapproval_123");
        assertThat(result.checkoutUrl()).contains("mercadopago.com.br/subscriptions/checkout");
        assertThat(result.amountCents()).isEqualTo(1290);
        server.verify();
    }

    @Test
    void shouldReadTheApprovedPaymentFromAnAuthorizedInvoice() {
        server.expect(requestTo("https://api.mercadopago.com/authorized_payments/456"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "id":456,
                          "preapproval_id":"preapproval_123",
                          "external_reference":"57de82d2-03bb-47c2-99d9-896d24cedc90",
                          "currency_id":"BRL",
                          "transaction_amount":99.90,
                          "payment":{"id":789,"status":"approved","status_detail":"accredited"}
                        }
                        """, MediaType.APPLICATION_JSON));

        var payment = gateway.getAuthorizedPayment("456");

        assertThat(payment.id()).isEqualTo("789");
        assertThat(payment.subscriptionId()).isEqualTo("preapproval_123");
        assertThat(payment.status()).isEqualTo("approved");
        assertThat(payment.amountCents()).isEqualTo(9990);
        server.verify();
    }
}
