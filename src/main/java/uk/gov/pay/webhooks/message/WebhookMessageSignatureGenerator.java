package uk.gov.pay.webhooks.message;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import static java.nio.charset.StandardCharsets.UTF_8;

public class WebhookMessageSignatureGenerator {

    private static final String HMAC_SHA256 = "HmacSHA256";

    public String generate(String body, String signingKey) throws InvalidKeyException {
        Mac mac = getMac();
        mac.init(new SecretKeySpec(signingKey.getBytes(UTF_8), HMAC_SHA256));
        byte[] signature = mac.doFinal(body.getBytes(UTF_8));
        return HexFormat.of().formatHex(signature);
    }

    private static Mac getMac() {
        try {
            return Mac.getInstance(HMAC_SHA256);
        } catch (NoSuchAlgorithmException e) {
            assert false : "This can never happen";
            throw new IllegalStateException(e);
        }
    }

}
