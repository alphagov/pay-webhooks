package uk.gov.pay.webhooks.deliveryqueue.managed;

import io.dropwizard.hibernate.UnitOfWork;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueDao;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueEntity;

import javax.inject.Inject;
import java.util.Optional;

public class WebhookMessagePollingService {
    private final WebhookDeliveryQueueDao webhookDeliveryQueueDao;
    private final SendAttempter sendAttempter;

    @Inject
    public WebhookMessagePollingService(WebhookDeliveryQueueDao webhookDeliveryQueueDao, SendAttempter sendAttempter) {
        this.webhookDeliveryQueueDao = webhookDeliveryQueueDao;
        this.sendAttempter = sendAttempter;
    }

    public void pollWebhookMessageQueue() {
        Optional<WebhookDeliveryQueueEntity> attemptCursor;

        do {
            attemptCursor = sendIfAvailable();
        } while(attemptCursor.isPresent());
    }

    @UnitOfWork
    protected Optional<WebhookDeliveryQueueEntity> sendIfAvailable() {
        return webhookDeliveryQueueDao.nextToSend()
                .map(webhookDeliveryQueueEntity -> {
                    sendAttempter.attemptSend(webhookDeliveryQueueEntity);
                    return webhookDeliveryQueueEntity;
                });
    }
}
