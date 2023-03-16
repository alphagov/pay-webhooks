package uk.gov.pay.webhooks.webhook.resource;

import com.fasterxml.jackson.databind.JsonNode;
import io.dropwizard.hibernate.UnitOfWork;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import uk.gov.pay.webhooks.deliveryqueue.DeliveryStatus;
import uk.gov.pay.webhooks.message.resource.WebhookDeliveryQueueResponse;
import uk.gov.pay.webhooks.message.resource.WebhookMessageResponse;
import uk.gov.pay.webhooks.message.resource.WebhookMessageSearchResponse;
import uk.gov.pay.webhooks.validations.WebhookRequestValidator;
import uk.gov.pay.webhooks.webhook.WebhookService;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;
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
import java.util.Optional;
import java.util.stream.StreamSupport;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/v1/webhook")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Tag(name = "Webhooks")
public class WebhookResource {

    private final WebhookService webhookService;
    private final WebhookRequestValidator webhookRequestValidator;

    @Inject
    public WebhookResource(WebhookService webhookService, WebhookRequestValidator webhookRequestValidator) {
        this.webhookService = webhookService;
        this.webhookRequestValidator = webhookRequestValidator;
    }

    @UnitOfWork
    @POST
    @Operation(
            summary = "Create new webhook",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = WebhookResponse.class))),
                    @ApiResponse(responseCode = "422", description = "Missing required parameters"),
                    @ApiResponse(responseCode = "400", description = "Invalid payload (ex: non existent event type)")
            }
    )
    public WebhookResponse createWebhook(@NotNull @Valid CreateWebhookRequest webhookRequest) {
        webhookRequestValidator.validate(webhookRequest);
        WebhookEntity webhookEntity = webhookService.createWebhook(webhookRequest);
        return WebhookResponse.from(webhookEntity);
    }

    @UnitOfWork
    @GET
    @Path("/{webhookExternalId}")
    @Operation(
            summary = "Get webhook by external ID and service ID (query param)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = WebhookResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Not found")
            }
    )
    public WebhookResponse getWebhookByExternalId(@Parameter(example = "gh0d0923jpsjdf0923jojlsfgkw3seg")
                                                  @PathParam("webhookExternalId") @NotNull String externalId,
                                                  @Parameter(example = "eo29upsdkjlk3jpwjj2dfn12")
                                                  @QueryParam("service_id") @NotNull String serviceId) {
        return webhookService
                .findByExternalIdAndServiceId(externalId, serviceId)
                .map(WebhookResponse::from)
                .orElseThrow(NotFoundException::new);
    }


    @UnitOfWork
    @GET
    @Path("/{webhookExternalId}/signing-key")
    @Operation(
            summary = "Get webhook signing key by external ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SigningKeyResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Not found")
            }
    )
    public SigningKeyResponse getSigningKeyByExternalId(@Parameter(example = "gh0d0923jpsjdf0923jojlsfgkw3seg")
                                                        @PathParam("webhookExternalId") @NotNull String externalId,
                                                        @Parameter(example = "eo29upsdkjlk3jpwjj2dfn12")
                                                        @QueryParam("service_id") @NotNull String serviceId) {
        return webhookService
                .findByExternalIdAndServiceId(externalId, serviceId)
                .map(WebhookEntity::getSigningKey)
                .map(SigningKeyResponse::new)
                .orElseThrow(NotFoundException::new);
    }

    @UnitOfWork
    @POST
    @Path("/{webhookExternalId}/signing-key")
    @Operation(
            summary = "Regenerate webhook signing key",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SigningKeyResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Not found")
            }
    )
    public SigningKeyResponse regenerateSigningKey(@Parameter(example = "gh0d0923jpsjdf0923jojlsfgkw3seg")
                                                   @PathParam("webhookExternalId") @NotNull String externalId,
                                                   @Parameter(example = "eo29upsdkjlk3jpwjj2dfn12")
                                                   @QueryParam("service_id") @NotNull String serviceId) {
        return webhookService.regenerateSigningKey(externalId, serviceId)
                .map(WebhookEntity::getSigningKey)
                .map(SigningKeyResponse::new)
                .orElseThrow(NotFoundException::new);
    }

    @UnitOfWork
    @GET
    @Operation(
            summary = "List webhooks for a service external ID or all webhooks",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = WebhookResponse.class)))),
                    @ApiResponse(responseCode = "400", description = "For invalid query params")
            }
    )
    public List<WebhookResponse> getWebhooks(@Parameter(example = "true", description = "Set to `true` to return live webhooks for service.")
                                             @NotNull @QueryParam("live") Boolean live,
                                             @Parameter(example = "eo29upsdkjlk3jpwjj2dfn12", description = "Service external ID. Required when override_service_id_restriction is not `true`")
                                             @QueryParam("service_id") String service_id,
                                             @Parameter(example = "true", description = "Set to true to list all webhooks. if 'true', service_id is not permitted")
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
    @Path("/{webhookExternalId}/message")
    @GET
    @Operation(
            summary = "Get webhook messages by webhook external ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = WebhookMessageSearchResponse.class)))
            }
    )
    public WebhookMessageSearchResponse getWebhookMessages(
            @Parameter(example = "gh0d0923jpsjdf0923jojlsfgkw3seg") @PathParam("webhookExternalId") String externalId,
            @Parameter(example = "1") @QueryParam("page") Integer page,
            @Parameter(example = "SUCCESSFUL") @Valid @QueryParam("status") DeliveryStatus deliveryStatus
    ) {
        var currentPage = Optional.ofNullable(page)
                .orElse(1);
        return webhookService.listMessages(externalId, deliveryStatus, currentPage);
    }

    @UnitOfWork
    @Path("/{webhookExternalId}/message/{webhookMessageExternalId}")
    @GET
    @Operation(
            summary = "Get messages by webhook external ID and message ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = WebhookMessageResponse.class)))
            }
    )
    public WebhookMessageResponse getWebhookMessage(
            @Schema(example = "gh0d0923jpsjdf0923jojlsfgkw3seg") @PathParam("webhookExternalId") String externalId,
            @Schema(example = "s0wjen129ejalk21nfjkdknf1jejklh") @PathParam("webhookMessageExternalId") String messageId
    ) {
        return webhookService.getMessage(externalId, messageId)
                .orElseThrow(NotFoundException::new);
    }

    @UnitOfWork
    @Path("/{webhookExternalId}/message/{webhookMessageExternalId}/attempt")
    @GET
    @Operation(
            summary = "Get message attempts for webhook external ID and message ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = WebhookDeliveryQueueResponse.class))))
            }
    )
    public List<WebhookDeliveryQueueResponse> getWebhookMessageAttemps(
            @Schema(example = "gh0d0923jpsjdf0923jojlsfgkw3seg") @PathParam("webhookExternalId") String externalId,
            @Schema(example = "s0wjen129ejalk21nfjkdknf1jejklh") @PathParam("webhookMessageExternalId") String messageId
    ) {
        return webhookService.listMessageAttempts(externalId, messageId);
    }

    @UnitOfWork
    @PATCH
    @Path("/{webhookExternalId}")
    @Operation(
            summary = "Update webhook",
            description = "Allows patching `description, callback_url, status, subscriptions`",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = WebhookResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid payload")
            }
    )
    public WebhookResponse updateWebhook(@Parameter(example = "gh0d0923jpsjdf0923jojlsfgkw3seg") @PathParam("webhookExternalId") @NotNull String externalId,
                                         @Parameter(example = "eo29upsdkjlk3jpwjj2dfn12") @QueryParam("service_id") @NotNull String serviceId,
                                         @ArraySchema(schema = @Schema(example = "{" +
                                                 "                            \"path\": \"description\"," +
                                                 "                            \"op\": \"replace\"," +
                                                 "                            \"value\": \"new description\"" +
                                                 "                        }"))
                                                 JsonNode payload) {
        var webhook = webhookService.findByExternalIdAndServiceId(externalId, serviceId).orElseThrow(NotFoundException::new);
        webhookRequestValidator.validate(payload, webhook.isLive());
        List<JsonPatchRequest> patchRequests = StreamSupport.stream(payload.spliterator(), false)
                .map(JsonPatchRequest::from)
                .toList();
        return WebhookResponse.from(webhookService.update(externalId, serviceId, patchRequests));
    }
}
