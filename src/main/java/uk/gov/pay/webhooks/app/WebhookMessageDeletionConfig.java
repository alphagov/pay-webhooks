package uk.gov.pay.webhooks.app;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class WebhookMessageDeletionConfig {
    
    @Valid
    @NotNull
    private int maxAgeOfMessages;
    
    @Valid
    @NotNull
    private int maxNumOfMessagesToExpire;

    public int getMaxAgeOfMessages() {
        return maxAgeOfMessages;
    }

    public int getMaxNumOfMessagesToExpire() {
        return maxNumOfMessagesToExpire;
    }
}
