package uk.gov.pay.webhooks.ledger;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import uk.gov.pay.webhooks.ledger.model.LedgerTransaction;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.okForJson;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;

public class LedgerStub {
    private final WireMockExtension wireMock;

    public LedgerStub(WireMockExtension wireMock) {
        this.wireMock = wireMock;
    }

    public void returnLedgerTransaction(String externalId, LedgerTransaction transaction) {
        var request = get("/v1/transaction/" + externalId + "?override_account_id_restriction=true");
        var response = okForJson(transaction);
        wireMock.stubFor(request.willReturn(response));
    }
}
