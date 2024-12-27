package uk.gov.pay.webhooks.app;

import jakarta.validation.Valid;

public class InternalRestClientConfig {
    @Valid
    private boolean disabledSecureConnection;

    public boolean isDisabledSecureConnection() {
        return disabledSecureConnection;
    }
}
