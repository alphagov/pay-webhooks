package uk.gov.pay.webhooks.queue.managed;

import io.dropwizard.hibernate.UnitOfWork;
import io.dropwizard.lifecycle.Managed;

public class WebhookMessageSender implements Managed {
    @Override
    public void start() throws Exception {
        
    }

    @Override
    public void stop() throws Exception {

    }
    
    @UnitOfWork
    public void sendWebhook() {
        
    }
}
