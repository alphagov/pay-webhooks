package uk.gov.pay.webhooks.validations;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.InternetDomainName;
import com.google.inject.Inject;
import uk.gov.pay.webhooks.app.WebhooksConfig;
import uk.gov.service.payments.commons.api.validation.JsonPatchRequestValidator;
import uk.gov.service.payments.commons.api.validation.PatchPathOperation;
import uk.gov.service.payments.commons.model.jsonpatch.JsonPatchOp;
import uk.gov.service.payments.commons.model.jsonpatch.JsonPatchRequest;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toUnmodifiableSet;
import static uk.gov.pay.webhooks.webhook.resource.WebhookResponse.FIELD_CALLBACK_URL;
import static uk.gov.pay.webhooks.webhook.resource.WebhookResponse.FIELD_DESCRIPTION;
import static uk.gov.pay.webhooks.webhook.resource.WebhookResponse.FIELD_STATUS;
import static uk.gov.pay.webhooks.webhook.resource.WebhookResponse.FIELD_SUBSCRIPTIONS;

public class WebhookRequestValidator {
    private static final Map<PatchPathOperation, Consumer<JsonPatchRequest>> patchOperationValidators = Map.of(
            new PatchPathOperation(FIELD_DESCRIPTION, JsonPatchOp.REPLACE), JsonPatchRequestValidator::throwIfValueNotString,
            new PatchPathOperation(FIELD_CALLBACK_URL, JsonPatchOp.REPLACE), RequestValidations::throwIfValueNotValidUrl,
            new PatchPathOperation(FIELD_STATUS, JsonPatchOp.REPLACE), RequestValidations::throwIfValueNotValidStatusEnum,
            new PatchPathOperation(FIELD_SUBSCRIPTIONS, JsonPatchOp.REPLACE), RequestValidations::throwIfValueNotValidSubscriptionsArray
    );
    private final JsonPatchRequestValidator patchRequestValidator = new JsonPatchRequestValidator(patchOperationValidators);

    private final Set<InternetDomainName> allowedDomains;

    @Inject
    public WebhookRequestValidator(WebhooksConfig webhooksConfig) {
        this.allowedDomains = webhooksConfig.getLiveDataAllowHosts().stream().map(InternetDomainName::from).collect(toUnmodifiableSet());
    }

    public void validateJsonPatch(JsonNode payload)  {
        patchRequestValidator.validate(payload);
    }

    public void validateUrlIsInLiveDomains(String callbackUrl) throws MalformedURLException {
        URL url = new URL(callbackUrl);
        if (InternetDomainName.isValid(url.getHost())) {
            var domain = InternetDomainName.from(url.getHost());
            while (domain.hasParent()) {
                if (allowedDomains.contains(domain)) {
                    return;
                }
                domain = domain.parent();
            }
        }
        throw new DomainNotOnAllowListException(url.getHost() + " is not in the allow list");
    }

}

