package uk.gov.pay.webhooks.validations;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.service.payments.commons.api.validation.JsonPatchRequestValidator;
import uk.gov.service.payments.commons.api.validation.PatchPathOperation;
import uk.gov.service.payments.commons.model.jsonpatch.JsonPatchOp;
import uk.gov.service.payments.commons.model.jsonpatch.JsonPatchRequest;

import java.util.Map;
import java.util.function.Consumer;

import static uk.gov.pay.webhooks.webhook.resource.WebhookResponse.FIELD_CALLBACK_URL;
import static uk.gov.pay.webhooks.webhook.resource.WebhookResponse.FIELD_DESCRIPTION;
import static uk.gov.pay.webhooks.webhook.resource.WebhookResponse.FIELD_STATUS;
import static uk.gov.pay.webhooks.webhook.resource.WebhookResponse.FIELD_SUBSCRIPTIONS;


public class WebhookRequestValidator {
    private static final Map<PatchPathOperation, Consumer<JsonPatchRequest>> patchOperationValidators = Map.of(
            new PatchPathOperation(FIELD_DESCRIPTION, JsonPatchOp.REPLACE), JsonPatchRequestValidator::throwIfValueNotString,
            new PatchPathOperation(FIELD_CALLBACK_URL, JsonPatchOp.REPLACE), RequestValidations::throwIfValueNotValidUrl,
            new PatchPathOperation(FIELD_STATUS, JsonPatchOp.REPLACE), RequestValidations::throwIfValueNotValidStatusEnum,
            new PatchPathOperation(FIELD_SUBSCRIPTIONS, JsonPatchOp.REPLACE), JsonPatchRequestValidator::throwIfValueNotString
    );
    private final JsonPatchRequestValidator patchRequestValidator = new JsonPatchRequestValidator(patchOperationValidators);

    public void validateJsonPatch(JsonNode payload)  {
        patchRequestValidator.validate(payload);
    }
}

