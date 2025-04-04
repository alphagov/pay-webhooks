package uk.gov.pay.webhooks.ledger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.webhooks.app.WebhooksConfig;
import uk.gov.pay.webhooks.ledger.model.LedgerTransaction;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {
    
    private static final String TRANSACTION_ID = "3rke415aam1pl1u3hvaljbcll3";
    
    private LedgerService ledgerService;
    
    @Mock
    private Client mockClient;

    @Mock
    private WebTarget mockWebTarget;

    @Mock
    private Invocation.Builder mockClientRequestInvocationBuilder;
    
    @Mock
    private WebhooksConfig mockWebhooksConfig;
    
    @Mock
    private Response mockResponse;

    @Mock
    private LedgerTransaction mockTransaction;
    
    @BeforeEach
    void setUp() {
        given(mockWebhooksConfig.getLedgerBaseUrl()).willReturn("https://ledger");

        when(mockWebTarget.request()).thenReturn(mockClientRequestInvocationBuilder);
        when(mockClientRequestInvocationBuilder.get()).thenReturn(mockResponse);
        
        ledgerService = new LedgerService(mockClient, mockWebhooksConfig);
    }

    @Test
    void getTransactionReturnsTransactionWithId() {
        var uri = URI.create("https://ledger/v1/transaction/" + TRANSACTION_ID + "?override_account_id_restriction=true");
        when(mockClient.target(uri)).thenReturn(mockWebTarget);

        given(mockResponse.getStatus()).willReturn(200);
        given(mockResponse.readEntity(LedgerTransaction.class)).willReturn(mockTransaction);

        Optional<LedgerTransaction> ledgerTransaction = ledgerService.getTransaction(TRANSACTION_ID);

        assertThat(ledgerTransaction.isPresent(), is(true));
        assertThat(ledgerTransaction.get(), is(mockTransaction));
    }

    @Test
    void getTransactionReturnsEmptyOptionalWhenLedger404s() {
        var uri = URI.create("https://ledger/v1/transaction/" + TRANSACTION_ID + "?override_account_id_restriction=true");
        when(mockClient.target(uri)).thenReturn(mockWebTarget);

        given(mockResponse.getStatus()).willReturn(404);

        Optional<LedgerTransaction> ledgerTransaction = ledgerService.getTransaction(TRANSACTION_ID);

        assertThat(ledgerTransaction.isPresent(), is(false));
    }
}
