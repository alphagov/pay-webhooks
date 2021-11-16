package uk.gov.pay.webhooks.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.webhooks.eventtype.EventTypeName;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeDao;
import uk.gov.pay.webhooks.ledger.LedgerService;
import uk.gov.pay.webhooks.ledger.model.LedgerTransaction;
import uk.gov.pay.webhooks.message.dao.WebhookMessageDao;
import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;
import uk.gov.pay.webhooks.queue.InternalEvent;
import uk.gov.pay.webhooks.util.ExternalIdGenerator;
import uk.gov.pay.webhooks.webhook.WebhookService;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;

import javax.inject.Inject;
import java.io.IOException;
import java.time.InstantSource;
import java.util.Date;

public class WebhookMessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookMessageService.class);

    private final WebhookService webhookService;
    private final LedgerService ledgerService;
    private final EventTypeDao eventTypeDao;
    private final InstantSource instantSource;
    private final ExternalIdGenerator externalIdGenerator;
    private final ObjectMapper objectMapper;
    private final WebhookMessageDao webhookMessageDao;

    @Inject
    public WebhookMessageService(WebhookService webhookService,
                                 LedgerService ledgerService, 
                                 EventTypeDao eventTypeDao,
                                 InstantSource instantSource,
                                 ExternalIdGenerator externalIdGenerator,
                                 ObjectMapper objectMapper,
                                 WebhookMessageDao webhookMessageDao) {
        this.webhookService = webhookService;
        this.ledgerService = ledgerService;
        this.eventTypeDao = eventTypeDao;
        this.instantSource = instantSource;
        this.externalIdGenerator = externalIdGenerator;
        this.objectMapper = objectMapper;
        this.webhookMessageDao = webhookMessageDao;
    }
    
    public void handleInternalEvent(InternalEvent event) throws IOException, InterruptedException {
        var subscribedWebhooks = webhookService.getWebhooksSubscribedToEvent(event);

        if (!subscribedWebhooks.isEmpty()) {
            LedgerTransaction ledgerTransaction = ledgerService.getTransaction(event.resourceExternalId()).orElseThrow(IllegalArgumentException::new);
            subscribedWebhooks
                    .stream()
                    .map(webhook -> buildWebhookMessage(webhook, event, ledgerTransaction))
                    .forEach(webhookMessageDao::create);
        }
    }

    private WebhookMessageEntity buildWebhookMessage(WebhookEntity webhook, InternalEvent event, LedgerTransaction ledgerTransaction) {
        JsonNode resource = objectMapper.valueToTree(ledgerTransaction); // will probably need some more transformation

        var webhookMessageEntity = new WebhookMessageEntity();
        webhookMessageEntity.setExternalId(externalIdGenerator.newExternalId());
        webhookMessageEntity.setCreatedDate(Date.from(instantSource.instant()));
        webhookMessageEntity.setWebhookEntity(webhook);
        webhookMessageEntity.setEventDate(Date.from(event.eventDate().toInstant()));
        webhookMessageEntity.setEventType(eventTypeDao.findByName(EventTypeName.of(event.eventType())).orElseThrow(IllegalArgumentException::new));
        webhookMessageEntity.setResource(resource);
        return webhookMessageEntity;
    }
}
