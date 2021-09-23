package uk.gov.pay.webhooks.webhook;

import uk.gov.pay.webhooks.webhook.dao.WebhookDao;
import uk.gov.pay.webhooks.webhook.entity.WebhookEntity;

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
    
    @POST
    public WebhookEntity createWebhook(@NotNull WebhookDTO webhookRequest) {
        return webhookDao.createWebhook(webhookRequest);
    }
}
