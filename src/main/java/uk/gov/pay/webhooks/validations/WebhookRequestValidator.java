package uk.gov.pay.webhooks.validations;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.pay.webhooks.util.Errors;
import uk.gov.service.payments.commons.api.validation.JsonPatchRequestValidator;
import uk.gov.service.payments.commons.api.validation.PatchPathOperation;
import uk.gov.service.payments.commons.api.validation.RequestValidator;
import uk.gov.service.payments.commons.model.jsonpatch.JsonPatchOp;
import uk.gov.service.payments.commons.model.jsonpatch.JsonPatchRequest;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static uk.gov.pay.webhooks.webhook.resource.WebhookResponse.FIELD_DESCRIPTION;


public class WebhookRequestValidator {
    private final RequestValidator requestValidator;
    private static final Map<PatchPathOperation, Consumer<JsonPatchRequest>> patchOperationValidators = Map.of(
            new PatchPathOperation(FIELD_DESCRIPTION, JsonPatchOp.REPLACE), JsonPatchRequestValidator::throwIfValueNotString
    );
    private final JsonPatchRequestValidator patchRequestValidator = new JsonPatchRequestValidator(patchOperationValidators);

    @Inject
    public WebhookRequestValidator(RequestValidator requestValidator) {
        this.requestValidator = requestValidator;
    }
    public Optional<Errors> validateUpdateRequest(JsonNode payload) {
        var errors = requestValidator.checkIsString(
                payload,
                FIELD_DESCRIPTION);

      var errorList = Errors.from(errors); 
      if (errorList.getErrors().isEmpty()) {
          return Optional.empty();
        }
      else {
          return Optional.of(errorList);
      }
    }

    public void validateJsonPatch(JsonNode payload) {
        patchRequestValidator.validate(payload);
    }
}

