package uk.gov.pay.webhooks.ledger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.okForJson;

public class LedgerStub {
    private final WireMockExtension wireMock;

    public LedgerStub(WireMockExtension wireMock) {
        this.wireMock = wireMock;
    }

    public void returnLedgerTransaction(TransactionFromLedgerFixture transaction) {
        var request = get("/v1/transaction/" + transaction.getTransactionId() + "?override_account_id_restriction=true");
        var response = okForJson(transaction);
        wireMock.stubFor(request.willReturn(response));
    }
}
