package uk.gov.pay.webhooks.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.pay.webhooks.deliveryqueue.WebhookNotActiveException;
import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;
import uk.gov.pay.webhooks.validations.CallbackUrlDomainNotOnAllowListException;
import uk.gov.pay.webhooks.validations.CallbackUrlService;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookStatus;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.InvalidKeyException;
import java.time.Duration;

public class WebhookMessageSender {

    public static final String SIGNATURE_HEADER_NAME = "Pay-Signature";
    public static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final HttpClient httpClient;
    private final WebhookMessageSignatureGenerator webhookMessageSignatureGenerator;
    private final CallbackUrlService callbackUrlService;
    private final ObjectMapper objectMapper;

    @Inject
    public WebhookMessageSender(HttpClient httpClient,
                                ObjectMapper objectMapper,
                                CallbackUrlService callbackUrlService,
                                WebhookMessageSignatureGenerator webhookMessageSignatureGenerator) {
        this.httpClient = httpClient;
        this.webhookMessageSignatureGenerator = webhookMessageSignatureGenerator;
        this.callbackUrlService = callbackUrlService;
        this.objectMapper = objectMapper;
    }

    public HttpResponse<String> sendWebhookMessage(WebhookMessageEntity webhookMessage) throws IOException, InterruptedException, InvalidKeyException, CallbackUrlDomainNotOnAllowListException {
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

        var httpRequest = HttpRequest.newBuilder(uri)
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .header(SIGNATURE_HEADER_NAME, signature)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
    }

}
