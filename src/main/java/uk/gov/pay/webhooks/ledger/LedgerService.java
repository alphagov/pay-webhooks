package uk.gov.pay.webhooks.ledger;

import uk.gov.pay.webhooks.app.WebhooksConfig;
import uk.gov.pay.webhooks.ledger.model.LedgerTransaction;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.Optional;

public class LedgerService {

    private final Client client;
    private final String ledgerUrl;

    @Inject
    public LedgerService(Client client, WebhooksConfig configuration) {
        this.client = client;
        this.ledgerUrl = configuration.getLedgerBaseUrl();
    }

    public Optional<LedgerTransaction> getTransaction(String id) {
        var uri = URI.create(ledgerUrl + "/v1/transaction/" + id + "?override_account_id_restriction=true");
        return getTransactionFromLedger(uri);
    }

    private Optional<LedgerTransaction> getTransactionFromLedger(URI uri) {
        Response response = getResponse(uri);
        if (response.getStatus() == 200) {
            return Optional.of(response.readEntity(LedgerTransaction.class));
        }
        return Optional.empty();
    }

    private Response getResponse(URI uri) {
        return client.target(uri).request().get();
    }
}
