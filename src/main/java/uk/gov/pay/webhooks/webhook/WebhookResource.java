package uk.gov.pay.webhooks.webhook;

import io.dropwizard.hibernate.UnitOfWork;
import uk.gov.pay.webhooks.webhook.entity.Webhook;
import uk.gov.pay.webhooks.webhook.entity.dao.WebhookDao;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/v1/webhook")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class WebhookResource {
    
    private final WebhookDao webhookDao;
    
    @Inject
    public WebhookResource(WebhookDao webhookDao) {
        this.webhookDao = webhookDao;
    }
    
    @UnitOfWork
    @POST
    public Webhook createWebhook(@NotNull @Valid CreateWebhookRequest webhookRequest) {
        var webhook = Webhook.from(webhookRequest);
        return webhookDao.create(webhook);
    }
    
    @UnitOfWork
    @GET
    @Path("/{externalId}")
    public Webhook getWebhookByExternalId(@PathParam ("externalId")@NotNull @Valid String externalId) {
        return webhookDao
                .findByExternalId(externalId)
                .orElseThrow(NotFoundException::new);
    }
}
