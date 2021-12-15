package uk.gov.pay.webhooks.message.dao;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;
import uk.gov.pay.webhooks.message.resource.WebhookMessageResponse;

import javax.inject.Inject;
import java.util.List;

public class WebhookMessageDao extends AbstractDAO<WebhookMessageEntity> {

    @Inject
    public WebhookMessageDao(SessionFactory factory) {
        super(factory);
        }
        
    public WebhookMessageEntity create(WebhookMessageEntity webhookMessage) {
        persist(webhookMessage);
        return webhookMessage;
    }
    
    public List<WebhookMessageResponse> search(String webhookId, int pageNumber, String status) {
        
    }
}
