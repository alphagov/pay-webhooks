package uk.gov.pay.webhooks.publicapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ThreeDSecure {

    @JsonProperty("required")
    private boolean required;

    public ThreeDSecure() {
    }

    public ThreeDSecure(boolean required) {
        this.required = required;
    }

    public boolean isRequired() {
        return required;
    }

}
