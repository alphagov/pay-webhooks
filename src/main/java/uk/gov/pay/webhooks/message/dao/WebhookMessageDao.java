package uk.gov.pay.webhooks.message.dao;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;
import uk.gov.pay.webhooks.message.resource.WebhookMessageResponse;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

public class WebhookMessageDao extends AbstractDAO<WebhookMessageEntity> {

    @Inject
    public WebhookMessageDao(SessionFactory factory) {
        super(factory);
        }
        
    public WebhookMessageEntity create(WebhookMessageEntity webhookMessage) {
        persist(webhookMessage);
        return webhookMessage;
    }
    
//    public List<WebhookMessageResponse> search(String webhookId, int pageNumber, String status) {
//        return namedTypedQuery(WebhookMessageEntity.SEARCH_BY_STATUS)
//                .getResultList()
//                .stream()
//                .map(WebhookMessageResponse::from)
//                .toList();
//    }

        public Optional<WebhookMessageEntity> getWebhookMessage(String webhookId) {
        return namedTypedQuery(WebhookMessageEntity.MESSAGES_BY_WEBHOOK_ID)
                .setParameter("external_id", webhookId)
                .getResultList()
                .stream()
                .findFirst();
    }
    
    
}
