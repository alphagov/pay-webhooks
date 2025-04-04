package uk.gov.pay.webhooks.ledger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okForContentType;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

public class LedgerStub {
    private final WireMockExtension wireMock;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LedgerStub(WireMockExtension wireMock) {
        this.wireMock = wireMock;
    }

    public void returnLedgerTransaction(TransactionFromLedgerFixture transaction) throws JsonProcessingException {
        // Construct the JSON ourselves because the standalone WireMock’s okForJson(…) does not
        // use our Jackson configuration (e.g. PropertyNamingStrategies.SnakeCaseStrategy)
        var json = objectMapper.writeValueAsString(transaction);
        var response = okForContentType(APPLICATION_JSON, json);
        var request = get("/v1/transaction/" + transaction.getTransactionId() + "?override_account_id_restriction=true");
        wireMock.stubFor(request.willReturn(response));
    }
}
