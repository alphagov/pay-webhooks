package uk.gov.pay.webhooks.util;

import java.math.BigInteger;
import java.security.SecureRandom;

public class ExternalIdGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public String newExternalId() {
        return new BigInteger(130, SECURE_RANDOM).toString(32);
    }

}
