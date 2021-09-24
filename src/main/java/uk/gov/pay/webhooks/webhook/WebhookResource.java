package uk.gov.pay.webhooks.webhook;

import io.dropwizard.hibernate.UnitOfWork;
import uk.gov.pay.webhooks.webhook.entity.WebhookEntity;
import uk.gov.pay.webhooks.webhook.entity.dao.WebhookDao;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/v1/webhook")
public class WebhookResource {
    
    private final WebhookDao webhookDao;
    
    @Inject
    public WebhookResource(WebhookDao webhookDao) {
        this.webhookDao = webhookDao;
    }
    
    @UnitOfWork
    @POST
    public long createWebhook(@NotNull WebhookDTO webhookRequest) {
        var webhook = WebhookEntity.from(webhookRequest);
        return webhookDao.create(webhook);
    }
}
