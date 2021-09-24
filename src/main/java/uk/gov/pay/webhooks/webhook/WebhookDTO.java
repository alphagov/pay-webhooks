package uk.gov.pay.webhooks.webhook;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

// responsible for: 
// - jackson serialiation/ validation from query params (POST BODY)
// - providing data for the entity to construct itself FROM (creation)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class WebhookDTO {
    private String serviceId;
    private String callbackUrl;
    private boolean live;
    private String description;
    
    public String getServiceId() {
        return serviceId;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public boolean isLive() {
        return live;
    }

    public String getDescription() {
        return description;
    }
}
