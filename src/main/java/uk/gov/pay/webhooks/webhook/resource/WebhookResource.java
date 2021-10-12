package uk.gov.pay.webhooks.webhook.resource;

import io.dropwizard.hibernate.UnitOfWork;
import uk.gov.pay.webhooks.webhook.WebhookService;
import uk.gov.pay.webhooks.webhook.dao.WebhookDao;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.noContent;

@Path("/v1/webhook")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class WebhookResource {
    
    private final WebhookService webhookService;
    
    @Inject
    public WebhookResource(WebhookService webhookService) {
        this.webhookService = webhookService;
    }
    
    @UnitOfWork
    @POST
    public WebhookResponse createWebhook(@NotNull @Valid CreateWebhookRequest webhookRequest) {
        WebhookEntity webhookEntity = webhookService.createWebhook(webhookRequest);
        return WebhookResponse.from(webhookEntity);
    }
    
    @UnitOfWork
    @GET
    @Path("/{externalId}")
    public WebhookResponse getWebhookByExternalId(@PathParam("externalId") @NotNull String externalId) {
        return webhookService
                .findByExternalId(externalId)
                .map(WebhookResponse::from)
                .orElseThrow(NotFoundException::new);
    }

    @UnitOfWork
    @DELETE
    @Path("/{externalId}")
    public Response deleteWebhook(@PathParam("externalId") @NotNull String externalId) {
        webhookService.deleteWebhook(externalId);
        return noContent().build();
    }
}
