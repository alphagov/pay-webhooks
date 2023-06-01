package uk.gov.pay.webhooks.app;

import javax.validation.Valid;

public class InternalRestClientConfig {
    @Valid
    private boolean disabledSecureConnection;

    public boolean isDisabledSecureConnection() {
        return disabledSecureConnection;
    }
}
