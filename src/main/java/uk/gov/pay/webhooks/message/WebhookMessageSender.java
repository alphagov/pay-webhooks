package uk.gov.pay.webhooks.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import uk.gov.pay.webhooks.deliveryqueue.WebhookNotActiveException;
import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;
import uk.gov.pay.webhooks.validations.CallbackUrlDomainNotOnAllowListException;
import uk.gov.pay.webhooks.validations.CallbackUrlService;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookStatus;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.security.InvalidKeyException;
import java.time.Duration;

public class WebhookMessageSender {

    public static final String SIGNATURE_HEADER_NAME = "Pay-Signature";
    public static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final CloseableHttpClient httpClient;
    private final WebhookMessageSignatureGenerator webhookMessageSignatureGenerator;
    private final CallbackUrlService callbackUrlService;
    private final ObjectMapper objectMapper;

    @Inject
    public WebhookMessageSender(CloseableHttpClient httpClient,
                                ObjectMapper objectMapper,
                                CallbackUrlService callbackUrlService,
                                WebhookMessageSignatureGenerator webhookMessageSignatureGenerator) {
        this.httpClient = httpClient;
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

        var request = new HttpPost(uri);
        request.addHeader("Content-Type", "application/json");
        request.addHeader(SIGNATURE_HEADER_NAME, signature);
        request.setEntity(new StringEntity(body));

        try(CloseableHttpResponse response = httpClient.execute(request)) {
            return response;
        }
    }

}
