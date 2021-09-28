package uk.gov.pay.webhooks.webhook;

import io.dropwizard.hibernate.UnitOfWork;
import uk.gov.pay.webhooks.webhook.entity.WebhookEntity;
import uk.gov.pay.webhooks.webhook.entity.dao.WebhookDao;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/v1/webhook")
public class WebhookResource {
    
    private final WebhookDao webhookDao;
    
    @Inject
    public WebhookResource(WebhookDao webhookDao) {
        this.webhookDao = webhookDao;
    }
    
    @UnitOfWork
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public long createWebhook(@NotNull @Valid CreateWebhookRequest webhookRequest) {
        var webhook = WebhookEntity.from(webhookRequest);
        return webhookDao.create(webhook);
    }
}
