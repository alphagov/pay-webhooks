package uk.gov.pay.webhooks.ledger;

import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.webhooks.app.WebhooksConfig;
import uk.gov.pay.webhooks.ledger.model.LedgerTransaction;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {
    
    private static final String TRANSACTION_ID = "3rke415aam1pl1u3hvaljbcll3";
    
    private static final String LEDGER_TRANSACTION_JSON = """
            {
              "amount": 1000,
              "state": {
                "finished": false,
                "status": "created"
              },
              "description": "Test description",
              "reference": "aReference",
              "language": "en",
              "transaction_id": "3rke415aam1pl1u3hvaljbcll3",
              "return_url": "https://example.org",
              "email": "someone@example.org",
              "payment_provider": "sandbox",
              "created_date": "2018-09-22T10:13:16.067Z",
              "card_details": {
                "cardholder_name": "j.doe@example.org",
                "billing_address": {
                  "line1": "line1",
                  "line2": "line2",
                  "postcode": "AB1 2CD",
                  "city": "London",
                  "country": "GB"
                },
                "card_brand": ""
              },
              "delayed_capture": false,
              "moto": false,
              "authorisation_summary": {
                "three_d_secure": {
                  "required": true,
                  "version": "2.1.0"
                }
              }
            }
            """;
    
    private LedgerService ledgerService;
    
    @Mock
    private HttpClient mockHttpClient;
    
    @Mock
    private WebhooksConfig mockWebhooksConfig;
    
    @Mock
    private HttpResponse<String> mockHttpResponse;
    
    @BeforeEach
    void setUp() {
        given(mockWebhooksConfig.getLedgerBaseUrl()).willReturn("https://ledger");
        
        ledgerService = new LedgerService(mockHttpClient, mockWebhooksConfig);
    }

    @Test
    void getTransactionReturnsTransactionWithId() throws IOException, InterruptedException {
        var uri = URI.create("https://ledger/v1/transaction/" + TRANSACTION_ID + "?override_account_id_restriction=true");
        given(mockHttpClient.send(eq(HttpRequest.newBuilder().uri(uri).build()), ArgumentMatchers.<BodyHandler<String>>any())).willReturn(mockHttpResponse);
        given(mockHttpResponse.statusCode()).willReturn(200);
        given(mockHttpResponse.body()).willReturn(LEDGER_TRANSACTION_JSON);

        Optional<LedgerTransaction> ledgerTransaction = ledgerService.getTransaction(TRANSACTION_ID);

        assertThat(ledgerTransaction.isPresent(), is(true));
        assertThat(ledgerTransaction.get().getTransactionId(), is(TRANSACTION_ID));
    }

    @Test
    void getTransactionReturnsEmptyOptionalWhenLedger404s() throws IOException, InterruptedException {
        var uri = URI.create("https://ledger/v1/transaction/" + TRANSACTION_ID + "?override_account_id_restriction=true");
        given(mockHttpClient.send(eq(HttpRequest.newBuilder().uri(uri).build()), ArgumentMatchers.<BodyHandler<String>>any())).willReturn(mockHttpResponse);
        given(mockHttpResponse.statusCode()).willReturn(404);

        Optional<LedgerTransaction> ledgerTransaction = ledgerService.getTransaction(TRANSACTION_ID);

        assertThat(ledgerTransaction.isPresent(), is(false));
    }
}
