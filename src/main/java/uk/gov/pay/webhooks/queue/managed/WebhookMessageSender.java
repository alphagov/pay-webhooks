package uk.gov.pay.webhooks.queue.managed;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.lifecycle.Managed;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.context.internal.ManagedSessionContext;
import uk.gov.pay.webhooks.message.dao.WebhookMessageDao;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.Date;


public class WebhookMessageSender implements Managed {

    private WebhookMessageDao webhookMessageDao;
    private final ObjectMapper objectMapper;
    private final InstantSource instantSource;
    private final SessionFactory sessionFactory;

    @Inject
    public WebhookMessageSender(WebhookMessageDao webhookMessageDao, ObjectMapper objectMapper, InstantSource instantSource, SessionFactory sessionFactory) {
        this.webhookMessageDao = webhookMessageDao;
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
                transaction.commit();
            } catch (Exception e) {
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
//        var nextToSend = webhookMessageDao.nextToSend(Date.from(Instant.now()));
//        if (nextToSend.isPresent()) {
//            var uri = URI.create(nextToSend.get().getWebhookEntity().getCallbackUrl());
//            var httpRequest = HttpRequest.newBuilder(uri)
//                    .timeout(Duration.ofSeconds(5))
//                    .header("Content-Type", "application/json")
//                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(nextToSend.get().getResource())))
//                    .build();
//            HttpResponse<String> response = null;
//            try {
//                response = HttpClient.newHttpClient().send(httpRequest, HttpResponse.BodyHandlers.ofString());
//            } catch (HttpTimeoutException e) {
//                        nextToSend.get().setSendAt(Date.from(instantSource.instant().plusSeconds(600)));
//                        WebhookDeliveryQueueEntity.from(
//                        nextToSend.get(),
//                        instantSource.instant(),
//                        "Request timed out",
//                        false
//                );
//            } catch (IOException | InterruptedException e) {
//            nextToSend.get().setSendAt(Date.from(instantSource.instant().plusSeconds(600)));
//           WebhookDeliveryQueueEntity.from(
//                   nextToSend.get(),
//                   instantSource.instant(),
//                   e.getMessage(),
//                   false
//           );
//            }
//            var statusCode = response.statusCode();
//            if (statusCode >= 200 && statusCode <= 299) {
//                nextToSend.get().setSendAt(null);
//                                WebhookDeliveryQueueEntity.from(
//                                        nextToSend.get(),
//                                        instantSource.instant(),
//                                        String.valueOf(statusCode),
//                                        true
//                                );
//            } else {
//                nextToSend.get().setSendAt(Date.from(instantSource.instant().plusSeconds(600)) );
//                WebhookDeliveryQueueEntity.from(
//                        nextToSend.get(),
//                        instantSource.instant(),
//                        String.valueOf(statusCode),
//                        false
//                );  
//            }
//        }
//        return nextToSend.isPresent();
//    }
        return false;
    }
}
