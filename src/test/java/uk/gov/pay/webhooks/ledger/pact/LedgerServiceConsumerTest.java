package uk.gov.pay.webhooks.ledger.pact;

import au.com.dius.pact.consumer.junit.PactVerification;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.webhooks.app.InternalRestClientConfig;
import uk.gov.pay.webhooks.app.InternalRestClientFactory;
import uk.gov.pay.webhooks.app.WebhooksConfig;
import uk.gov.pay.webhooks.ledger.LedgerService;
import uk.gov.pay.webhooks.ledger.model.LedgerTransaction;
import uk.gov.service.payments.commons.testing.pact.consumers.Pacts;
import uk.gov.service.payments.commons.testing.pact.consumers.PayPactProviderRule;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LedgerServiceConsumerTest {

    @Rule
    public PayPactProviderRule ledgerRule = new PayPactProviderRule("ledger", this);

    @Mock
    WebhooksConfig configuration;
    @Mock
    InternalRestClientConfig internalRestClientConfig;

    private LedgerService ledgerService;

    @Before
    public void setUp() {
        when(internalRestClientConfig.isDisabledSecureConnection()).thenReturn(true);
        when(configuration.getLedgerBaseUrl()).thenReturn(ledgerRule.getUrl());

        ledgerService = new LedgerService(InternalRestClientFactory.buildClient(internalRestClientConfig), configuration);
    }

    @Test
    @PactVerification("ledger")
    @Pacts(pacts = {"webhooks-ledger-get-payment-transaction"})
    public void getTransaction_shouldSerialiseLedgerPaymentTransactionCorrectly() {
        String externalId = "e8eq11mi2ndmauvb51qsg8hccn";
        Optional<LedgerTransaction> mayBeTransaction = ledgerService.getTransaction(externalId);

        assertThat(mayBeTransaction.isPresent(), is(true));
        LedgerTransaction transaction = mayBeTransaction.get();
        assertThat(transaction.getTransactionId(), is(externalId));
        assertThat(transaction.getAmount(), is(12000L));
        assertThat(transaction.getGatewayAccountId(), is("3"));
        assertThat(transaction.getCredentialExternalId(), is("credential-external-id"));
    }
}
