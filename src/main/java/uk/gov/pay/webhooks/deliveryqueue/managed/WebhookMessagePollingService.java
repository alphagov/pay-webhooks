package uk.gov.pay.webhooks.deliveryqueue.managed;

import org.hibernate.SessionFactory;
import org.hibernate.context.internal.ManagedSessionContext;
import org.slf4j.MDC;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueDao;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueEntity;
import uk.gov.pay.webhooks.message.WebhookMessageSender;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.pay.webhooks.app.WebhooksKeys.JOB_BATCH_ID;
import static uk.gov.pay.webhooks.app.WebhooksKeys.WEBHOOK_EXTERNAL_ID;
import static uk.gov.pay.webhooks.app.WebhooksKeys.WEBHOOK_MESSAGE_EXTERNAL_ID;
import static uk.gov.pay.webhooks.app.WebhooksKeys.WEBHOOK_MESSAGE_RESOURCE_EXTERNAL_ID;
import static uk.gov.service.payments.logging.LoggingKeys.MDC_REQUEST_ID_KEY;

public class WebhookMessagePollingService {
    private final WebhookDeliveryQueueDao webhookDeliveryQueueDao;

    private final SendAttempter sendAttempter;

    private final SessionFactory sessionFactory;

    @Inject
    public WebhookMessagePollingService(WebhookDeliveryQueueDao webhookDeliveryQueueDao, SendAttempter sendAttempter, SessionFactory sessionFactory) {
        this.webhookDeliveryQueueDao = webhookDeliveryQueueDao;
        this.sendAttempter = sendAttempter;
        this.sessionFactory = sessionFactory;
    }

    public void pollWebhookMessageQueue() {
        Optional<WebhookDeliveryQueueEntity> attemptCursor;
        MDC.put(JOB_BATCH_ID, UUID.randomUUID().toString());

        do {
            attemptCursor = sendIfAvailable();
        } while(attemptCursor.isPresent());
        MDC.remove(JOB_BATCH_ID);
    }

    private Optional<WebhookDeliveryQueueEntity> sendIfAvailable() {
        MDC.put(MDC_REQUEST_ID_KEY, UUID.randomUUID().toString());
        var session = sessionFactory.openSession();
        ManagedSessionContext.bind(session);
        var transaction = session.beginTransaction();
        try {
            return webhookDeliveryQueueDao.nextToSend()
                    .map(webhookDeliveryQueueEntity -> {
                        MDC.put(WEBHOOK_EXTERNAL_ID, webhookDeliveryQueueEntity.getWebhookMessageEntity().getWebhookEntity().getExternalId());
                        MDC.put(WEBHOOK_MESSAGE_RESOURCE_EXTERNAL_ID, webhookDeliveryQueueEntity.getWebhookMessageEntity().getResourceExternalId());
                        MDC.put(WEBHOOK_MESSAGE_EXTERNAL_ID, webhookDeliveryQueueEntity.getWebhookMessageEntity().getExternalId());
                        sendAttempter.attemptSend(webhookDeliveryQueueEntity);
                        transaction.commit();
                        return webhookDeliveryQueueEntity;
                    });
        } catch (Exception e) {
           transaction.rollback();
           return Optional.empty();
        } finally {
            ManagedSessionContext.unbind(sessionFactory);
            List.of(MDC_REQUEST_ID_KEY, WEBHOOK_EXTERNAL_ID, WEBHOOK_MESSAGE_RESOURCE_EXTERNAL_ID).forEach(MDC::remove);
        }
    }
}
