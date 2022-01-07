package uk.gov.pay.webhooks.webhook.resource;

import com.fasterxml.jackson.databind.JsonNode;
import io.dropwizard.hibernate.UnitOfWork;
import uk.gov.pay.webhooks.message.resource.WebhookMessageResponse;
import uk.gov.pay.webhooks.validations.WebhookRequestValidator;
import uk.gov.pay.webhooks.webhook.WebhookService;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;
import uk.gov.service.payments.commons.api.exception.ValidationException;
import uk.gov.service.payments.commons.model.jsonpatch.JsonPatchRequest;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import java.util.List;
import java.util.stream.StreamSupport;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/v1/webhook")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class WebhookResource {
    
    private final WebhookService webhookService;
    private final WebhookRequestValidator webhookRequestValidator;
    
    @Inject
    public WebhookResource(WebhookService webhookService) {
        this.webhookService = webhookService;
        this.webhookRequestValidator =  new WebhookRequestValidator();
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
    public WebhookResponse getWebhookByExternalId(@PathParam("externalId") @NotNull String externalId,
                                                  @QueryParam("service_id") @NotNull String serviceId) {
        return webhookService
                .findByExternalId(externalId, serviceId)
                .map(WebhookResponse::from)
                .orElseThrow(NotFoundException::new);
    }


    @UnitOfWork
    @GET
    @Path("/{externalId}/signing-key")
    public SigningKeyResponse getSigningKeyByExternalId(@PathParam("externalId") @NotNull String externalId,
                                                        @QueryParam("service_id") @NotNull String serviceId) {
        return webhookService
                .findByExternalId(externalId, serviceId)
                .map(WebhookEntity::getSigningKey)
                .map(SigningKeyResponse::new)
                .orElseThrow(NotFoundException::new);
    }
    
    @UnitOfWork
    @POST
    @Path("/{externalId}/signing-key")
    public SigningKeyResponse regenerateSigningKey(@PathParam("externalId") @NotNull String externalId,
                                                   @QueryParam("service_id") @NotNull String serviceId) {
        return webhookService.regenerateSigningKey(externalId, serviceId)
                .map(WebhookEntity::getSigningKey)
                .map(SigningKeyResponse::new)
                .orElseThrow(NotFoundException::new);
    }

    @UnitOfWork
    @GET
    public List<WebhookResponse> getWebhooks(@NotNull @QueryParam("live") Boolean live,
                                             @QueryParam("service_id") String service_id,
                                             @QueryParam("override_service_id_restriction") boolean overrideServiceIdRestriction) {
            if (service_id != null && overrideServiceIdRestriction) {
                throw new BadRequestException("service_id not permitted when using override_service_id_restriction");
            }
            
            if (service_id == null && !overrideServiceIdRestriction) {
                throw new BadRequestException("either service_id or override_service_id_restriction query parameter must be provided");
            }
            
            List<WebhookEntity> results = overrideServiceIdRestriction ? webhookService.list(live) : webhookService.list(live, service_id);

            return results
                    .stream()
                    .map(WebhookResponse::from)
                    .toList();
    }

    @UnitOfWork
    @Path("/{externalId}/messages")
    @GET
    public List<WebhookMessageResponse> getWebhookMessages(@PathParam("externalId") String externalId) {
        return  webhookService.listMessages(externalId)
                .stream()
                .map(WebhookMessageResponse::from)
                .toList();
    }

    @UnitOfWork
    @PATCH
    @Path("/{externalId}")
    public WebhookResponse updateWebhook(@PathParam("externalId") @NotNull String externalId, 
                                  @QueryParam("service_id") @NotNull String serviceId, 
                                  JsonNode payload) {
        try {
            webhookRequestValidator.validateJsonPatch(payload);
        }
        catch (ValidationException e) {
            throw new BadRequestException(String.join(", ", e.getErrors()));
        }
        List<JsonPatchRequest> patchRequests = StreamSupport.stream(payload.spliterator(), false)
                .map(JsonPatchRequest::from)
                .toList();
        return WebhookResponse.from(webhookService.update(externalId, serviceId, patchRequests));
    }
    

}
