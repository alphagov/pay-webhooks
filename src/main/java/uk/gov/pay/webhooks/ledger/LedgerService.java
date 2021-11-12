package uk.gov.pay.webhooks.ledger;

import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.pay.webhooks.app.WebhooksConfig;
import uk.gov.pay.webhooks.ledger.model.LedgerTransaction;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

public class LedgerService {

    private final HttpClient httpClient;
    private final String ledgerUrl;

    @Inject
    public LedgerService(HttpClient httpClient, WebhooksConfig configuration) {
        this.httpClient = httpClient;
        this.ledgerUrl = configuration.getLedgerBaseUrl();
    }

    public Optional<LedgerTransaction> getTransaction(String id) throws IOException, InterruptedException {
        var uri = URI.create(ledgerUrl + "/v1/transaction/" + id + "?override_account_id_restriction=true");
        return getTransactionFromLedger(uri);
    }

    private Optional<LedgerTransaction> getTransactionFromLedger(URI uri) throws IOException, InterruptedException {
        HttpResponse<String> response = getResponse(uri);
        if (response.statusCode() == 200) {
            var objectMapper = new ObjectMapper();
            return Optional.of(objectMapper.readValue(response.body(), LedgerTransaction.class));
        }
        return Optional.empty();
    }

    private HttpResponse<String> getResponse(URI uri) throws IOException, InterruptedException {
        var httpRequest = HttpRequest.newBuilder().uri(uri).build();
        return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(UTF_8));
    }
}
