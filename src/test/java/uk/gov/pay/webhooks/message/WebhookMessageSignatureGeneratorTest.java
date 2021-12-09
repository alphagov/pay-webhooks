package uk.gov.pay.webhooks.message;

import org.junit.jupiter.api.Test;

import java.security.InvalidKeyException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

class WebhookMessageSignatureGeneratorTest {

    private final WebhookMessageSignatureGenerator signatureGenerator = new WebhookMessageSignatureGenerator();

    @Test
    void generatesSignature() throws InvalidKeyException {
        String signature = signatureGenerator.generate("We captured a £10 payment! 💷", "This is my secret key! 🔑");
        assertThat(signature, is("a678c8326237dc08acabed4ec3948ee82f94c4b35cdc44b68f26fa8ef595efff")); // pragma: allowlist secret
    }

}
