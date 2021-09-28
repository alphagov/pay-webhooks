package uk.gov.pay.webhooks.webhook;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class CreateWebhookRequest { 
    @NotEmpty
    @Size(max = 30)
    private String serviceId;

    @NotNull
    private Boolean live;
    
    @NotEmpty
    @Size(max = 2048)
    private String callbackUrl;
    
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
