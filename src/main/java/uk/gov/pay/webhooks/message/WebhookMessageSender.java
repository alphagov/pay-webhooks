package uk.gov.pay.webhooks.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import uk.gov.pay.webhooks.deliveryqueue.WebhookNotActiveException;
import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;
import uk.gov.pay.webhooks.validations.CallbackUrlDomainNotOnAllowListException;
import uk.gov.pay.webhooks.validations.CallbackUrlService;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookStatus;

import java.io.IOException;
import java.net.URI;
import java.security.InvalidKeyException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class WebhookMessageSender {

    public static final String SIGNATURE_HEADER_NAME = "Pay-Signature";

    private final CloseableHttpClient httpClient;
    private final WebhookMessageSignatureGenerator webhookMessageSignatureGenerator;
    private final CallbackUrlService callbackUrlService;
    private final ObjectMapper objectMapper;
    private final HttpPostFactory httpPostFactory;

    @Inject
    public WebhookMessageSender(CloseableHttpClient httpClient,
                                HttpPostFactory httpPostFactory,
                                ObjectMapper objectMapper,
                                CallbackUrlService callbackUrlService,
                                WebhookMessageSignatureGenerator webhookMessageSignatureGenerator) {
        this.httpClient = httpClient;
        this.httpPostFactory = httpPostFactory;
        this.webhookMessageSignatureGenerator = webhookMessageSignatureGenerator;
        this.callbackUrlService = callbackUrlService;
        this.objectMapper = objectMapper;
    }

    public CloseableHttpResponse sendWebhookMessage(WebhookMessageEntity webhookMessage) throws IOException, InterruptedException, InvalidKeyException, CallbackUrlDomainNotOnAllowListException {
        var webhook = webhookMessage.getWebhookEntity();

        if (webhook.isLive()) {
            callbackUrlService.validateCallbackUrl(webhook.getCallbackUrl(), webhook.isLive());
        }
        if (webhook.getStatus() != WebhookStatus.ACTIVE) {
            throw new WebhookNotActiveException("Webhook must be active to send messages");
        }

        URI uri = URI.create(webhookMessage.getWebhookEntity().getCallbackUrl());
        String body = objectMapper.writeValueAsString(WebhookMessageBody.from(webhookMessage));
        String signingKey = webhookMessage.getWebhookEntity().getSigningKey();
        String signature = webhookMessageSignatureGenerator.generate(body, signingKey);

        var request = httpPostFactory.newHttpPost(uri);
        request.addHeader("Content-Type", "application/json");
        request.addHeader(SIGNATURE_HEADER_NAME, signature);
        request.setEntity(new StringEntity(body, UTF_8));

        try(CloseableHttpResponse response = httpClient.execute(request)) {
            return response;
        }
    }

}
