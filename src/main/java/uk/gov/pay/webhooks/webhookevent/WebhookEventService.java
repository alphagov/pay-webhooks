package uk.gov.pay.webhooks.webhookevent;

import io.dropwizard.hibernate.UnitOfWork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.webhooks.eventtype.EventTypeName;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeEntity;
import uk.gov.pay.webhooks.queue.Event;
import uk.gov.pay.webhooks.queue.EventMessageHandler;
import uk.gov.pay.webhooks.webhook.WebhookService;

import javax.inject.Inject;
import java.util.Optional;

public class WebhookEventService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventMessageHandler.class);

    private final  WebhookService webhookService;

    @Inject
    public WebhookEventService(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    public boolean handleInternalMessage(Event event) {
        if (serviceIsSubscribedToEvent(event)) {
            publish(event);
            return true;
        } else {
            LOGGER.info("Ignoring event %s".formatted(event));
            return false;
        }
    }

    private void publish(Event event) {
        LOGGER.info("Publishing %s".formatted(event));
    }

    private boolean serviceIsSubscribedToEvent(Event event) {
        return webhookService.findByServiceId(event.getServiceId())
                .map(webhookEntity -> webhookEntity.getSubscriptions().stream()
                        .map(EventTypeEntity::getName)
                        .map(EventMapper::getInternalEventNameFor)
                        .flatMap(Optional::stream)
                        .toList()
                        .contains(event.getEventType()))
                .orElse(false);
    }
}
