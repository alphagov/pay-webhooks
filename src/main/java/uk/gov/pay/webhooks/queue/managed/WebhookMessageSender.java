package uk.gov.pay.webhooks.queue.managed;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.hibernate.UnitOfWork;
import io.dropwizard.hibernate.UnitOfWorkAwareProxyFactory;
import io.dropwizard.lifecycle.Managed;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.context.internal.ManagedSessionContext;
import uk.gov.pay.webhooks.app.WebhooksConfig;
import uk.gov.pay.webhooks.message.dao.entity.WebhookDeliveryAttemptEntity;
import uk.gov.pay.webhooks.webhook.WebhookService;

import javax.inject.Inject;
import javax.ws.rs.Path;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.InstantSource;
import java.util.Date;


public class WebhookMessageSender implements Managed {
    
    private final WebhookService webhookService;
    private final ObjectMapper objectMapper;
    private final InstantSource instantSource;
    private  final SessionFactory sessionFactory;

    @Inject
    public WebhookMessageSender(WebhookService webhookService, ObjectMapper objectMapper, InstantSource instantSource, SessionFactory sessionFactory) {
        this.webhookService = webhookService;
        this.objectMapper = objectMapper;
        this.instantSource = instantSource; 
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void start() throws Exception {
        Session session = sessionFactory.openSession();
        try (session) {
            ManagedSessionContext.bind(session);
            Transaction transaction = session.beginTransaction();
            try {
                sendWebhook();
            }
            catch (Exception e) {
                transaction.rollback();
                throw new RuntimeException(e);
            } finally {
                session.close();
                ManagedSessionContext.unbind(sessionFactory);
            }
     }
}

    @Override
    public void stop() throws Exception {

    }
    
    
    private boolean sendWebhook() throws IOException, InterruptedException {
        var nextToSend = webhookService.nextWebhookMessageToSend();
        if (nextToSend.isPresent()) {
            var uri = URI.create(nextToSend.get().getWebhookEntity().getCallbackUrl());
            var httpRequest = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(nextToSend.get().getResource())))
                    .build();
            HttpResponse<String> response = null;
            try {
                response = HttpClient.newHttpClient().send(httpRequest, HttpResponse.BodyHandlers.ofString());
            } catch (HttpTimeoutException e) {
                        nextToSend.get().setSendAt(Date.from(instantSource.instant().plusSeconds(600)));
                        WebhookDeliveryAttemptEntity.from(
                        nextToSend.get(),
                        instantSource.instant(),
                        "Request timed out",
                        false
                );
            } catch (IOException | InterruptedException e) {
            nextToSend.get().setSendAt(Date.from(instantSource.instant().plusSeconds(600)));
            WebhookDeliveryAttemptEntity.from(
                    nextToSend.get(),
                    instantSource.instant(),
                    e.getMessage(),
                    false
            );
            }
            var statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode <= 299) {
                nextToSend.get().setSendAt(null);
                WebhookDeliveryAttemptEntity.from(
                        nextToSend.get(),
                        instantSource.instant(),
                        String.valueOf(statusCode),
                        true
                );
            } else {
                nextToSend.get().setSendAt(Date.from(instantSource.instant().plusSeconds(600)) );
                WebhookDeliveryAttemptEntity.from(
                        nextToSend.get(),
                        instantSource.instant(),
                        String.valueOf(statusCode),
                        false
                );  
            }
        }
        return nextToSend.isPresent();
    }
}
