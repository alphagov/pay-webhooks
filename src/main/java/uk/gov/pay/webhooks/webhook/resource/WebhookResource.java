package uk.gov.pay.webhooks.webhook.resource;

import io.dropwizard.hibernate.UnitOfWork;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;
import uk.gov.pay.webhooks.webhook.dao.WebhookDao;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

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
    public WebhookResponse createWebhook(@NotNull @Valid CreateWebhookRequest webhookRequest) {
        var webhookEntity = webhookDao.create(WebhookEntity.from(webhookRequest));
        return WebhookResponse.from(webhookEntity);
    }
    
    @UnitOfWork
    @GET
    @Path("/{externalId}")
    public WebhookResponse getWebhookByExternalId(@PathParam("externalId") @NotNull String externalId) {
        return webhookDao
                .findByExternalId(externalId)
                .map(WebhookResponse::from)
                .orElseThrow(NotFoundException::new);
    }
}
