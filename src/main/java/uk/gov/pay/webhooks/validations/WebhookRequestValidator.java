package uk.gov.pay.webhooks.validations;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import org.slf4j.MDC;
import uk.gov.pay.webhooks.webhook.resource.CreateWebhookRequest;
import uk.gov.service.payments.commons.api.validation.JsonPatchRequestValidator;
import uk.gov.service.payments.commons.api.validation.PatchPathOperation;
import uk.gov.service.payments.commons.model.jsonpatch.JsonPatchOp;
import uk.gov.service.payments.commons.model.jsonpatch.JsonPatchRequest;

import java.util.Map;

import static uk.gov.pay.webhooks.webhook.resource.WebhookResponse.FIELD_CALLBACK_URL;
import static uk.gov.pay.webhooks.webhook.resource.WebhookResponse.FIELD_DESCRIPTION;
import static uk.gov.pay.webhooks.webhook.resource.WebhookResponse.FIELD_STATUS;
import static uk.gov.pay.webhooks.webhook.resource.WebhookResponse.FIELD_SUBSCRIPTIONS;


public class WebhookRequestValidator {
    private final CallbackUrlService callbackUrlService;

    private final JsonPatchRequestValidator liveContextValidator = validator(true);
    private final JsonPatchRequestValidator testContextValidator = validator(false);

    @Inject
    public WebhookRequestValidator(CallbackUrlService callbackUrlService) {
        this.callbackUrlService = callbackUrlService;
    }

    public void validate(JsonNode payload, Boolean isLiveContext)  {
        if (isLiveContext) {
            liveContextValidator.validate(payload);
        } else {
            testContextValidator.validate(payload);
        }
    }

    // @TODO(sfount): create request can use a declarative Hibernate "Custom constraint validator" with annotation to validate the
    //                callback url. This will require guice injection in the validator contexts and is out of scoped here
    //                but would allow for all of the validation to be processed in one place
    public void validate(CreateWebhookRequest createWebhookRequest) {
        MDC.put("gateway_account_id", createWebhookRequest.gatewayAccountId());
        MDC.put("service_id", createWebhookRequest.serviceId());
        MDC.put("live", createWebhookRequest.live().toString());
        try {
            callbackUrlService.validateCallbackUrl(createWebhookRequest.callbackUrl(), createWebhookRequest.live());
        } finally {
            MDC.clear();
        }
    }

    private JsonPatchRequestValidator validator(Boolean isLiveContext) {
        return new JsonPatchRequestValidator(
                Map.of(
                        new PatchPathOperation(FIELD_DESCRIPTION, JsonPatchOp.REPLACE), JsonPatchRequestValidator::throwIfValueNotString,
                        new PatchPathOperation(FIELD_STATUS, JsonPatchOp.REPLACE), RequestValidations::throwIfValueNotValidStatusEnum,
                        new PatchPathOperation(FIELD_SUBSCRIPTIONS, JsonPatchOp.REPLACE), RequestValidations::throwIfValueNotValidSubscriptionsArray,
                        new PatchPathOperation(FIELD_CALLBACK_URL, JsonPatchOp.REPLACE), (jsonPatchRequest) -> { throwIfCallbackUrlNotValid(jsonPatchRequest, isLiveContext); }
                )
        );
    }

    private void throwIfCallbackUrlNotValid(JsonPatchRequest jsonPatchRequest, Boolean isLiveContext) {
        callbackUrlService.validateCallbackUrl(jsonPatchRequest.valueAsString(), isLiveContext);
    }
}

