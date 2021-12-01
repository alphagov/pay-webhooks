package uk.gov.pay.webhooks.queue.managed;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.hibernate.UnitOfWork;
import io.dropwizard.lifecycle.Managed;
import uk.gov.pay.webhooks.message.dao.entity.WebhookDeliveryAttemptEntity;
import uk.gov.pay.webhooks.webhook.WebhookService;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.InstantSource;
import java.util.Date;

public class WebhookMessageSender implements Managed {
    
    private final WebhookService webhookService;
    private final ObjectMapper objectMapper;
    private final InstantSource instantSource;

    @Inject
    public WebhookMessageSender(WebhookService webhookService, ObjectMapper objectMapper, InstantSource instantSource) {
        this.webhookService = webhookService;
        this.objectMapper = objectMapper;
        this.instantSource = instantSource;
    }

    @Override
    public void start() throws Exception {
            
    }

    @Override
    public void stop() throws Exception {

    }
    
    @UnitOfWork
    private void sendWebhook() throws IOException, InterruptedException {
        var nextToSend = webhookService.nextWebhookMessageToSend();
        if (nextToSend.isPresent()) {
            var uri = URI.create(nextToSend.get().getWebhookEntity().getCallbackUrl());
            var httpRequest = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(nextToSend.get().getResource())))
                    .build();
            HttpResponse<String> response = null;
            try {
                response = HttpClient.newHttpClient().send(httpRequest, HttpResponse.BodyHandlers.ofString());
            } catch (HttpTimeoutException e) {
                        nextToSend.get().setSendAt(Date.from(instantSource.instant().plusSeconds(600)));
                        WebhookDeliveryAttemptEntity.from(
                        nextToSend.get(),
                        instantSource.instant(),
                        "Request timed out",
                        false
                );
            } catch (IOException | InterruptedException e) {
            nextToSend.get().setSendAt(Date.from(instantSource.instant().plusSeconds(600)));
            WebhookDeliveryAttemptEntity.from(
                    nextToSend.get(),
                    instantSource.instant(),
                    e.getMessage(),
                    false
            );
            }
            var statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode <= 299) {
                nextToSend.get().setSendAt(null);
                WebhookDeliveryAttemptEntity.from(
                        nextToSend.get(),
                        instantSource.instant(),
                        String.valueOf(statusCode),
                        true
                );
            } else {
                nextToSend.get().setSendAt(Date.from(instantSource.instant().plusSeconds(600)) );
                WebhookDeliveryAttemptEntity.from(
                        nextToSend.get(),
                        instantSource.instant(),
                        String.valueOf(statusCode),
                        false
                );  
            }
        }
    }
}
