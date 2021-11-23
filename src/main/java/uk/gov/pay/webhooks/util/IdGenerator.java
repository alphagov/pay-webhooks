package uk.gov.pay.webhooks.util;

import java.math.BigInteger;
import java.security.SecureRandom;

public class IdGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String PREFIX_TEST = "webhook_test_";
    private static final String PREFIX_LIVE = "webhook_live_";

    public String newExternalId() {
        return new BigInteger(130, SECURE_RANDOM).toString(32);
    }

    public String newWebhookSigningKey(boolean live) {
        return (live ? PREFIX_LIVE : PREFIX_TEST) + newExternalId();
    }

}
