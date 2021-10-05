package uk.gov.pay.webhooks.webhookevent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.hibernate.UnitOfWork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.webhooks.eventtype.EventTypeName;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeEntity;
import uk.gov.pay.webhooks.queue.Event;
import uk.gov.pay.webhooks.queue.EventMessageHandler;
import uk.gov.pay.webhooks.webhook.WebhookService;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

public class WebhookEventService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventMessageHandler.class);

    private final  WebhookService webhookService;

    @Inject
    public WebhookEventService(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    public void handleInternalMessage(Event event) {
        serviceIsSubscribedToEvent(event).stream().forEach(webhookEntity -> publish(event, webhookEntity));
    }

    private void publish(Event event, WebhookEntity webhookEntity) {
        LOGGER.info("Publishing %s".formatted(event));
        var url = webhookEntity.getCallbackUrl();
        var jacksonObjectMapper = new ObjectMapper();
        var httpClient = HttpClient.newHttpClient();
        try {
            var request = HttpRequest.newBuilder(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jacksonObjectMapper.writeValueAsString(event)))
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    private Optional<WebhookEntity> serviceIsSubscribedToEvent(Event event) {
        return webhookService.findByServiceId(event.getServiceId())
                .filter(webhookEntity -> webhookEntity.getSubscriptions().stream()
                        .map(EventTypeEntity::getName)
                        .map(EventMapper::getInternalEventNameFor)
                        .flatMap(Optional::stream)
                        .toList()
                        .contains(event.getEventType()));
    }
}
