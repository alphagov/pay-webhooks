package uk.gov.pay.webhooks.publicapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthorisationSummary {

    @JsonProperty("three_d_secure")
    private ThreeDSecure threeDSecure;

    public AuthorisationSummary() {
    }

    public AuthorisationSummary(ThreeDSecure threeDSecure) {
        this.threeDSecure = threeDSecure;
    }

    public ThreeDSecure getThreeDSecure() {
        return threeDSecure;
    }
}
