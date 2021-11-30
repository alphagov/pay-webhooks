package uk.gov.pay.webhooks.message.dao;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;

import javax.inject.Inject;
import java.util.Date;
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
    
    public Optional<WebhookMessageEntity> nextToSend(Date sendAt){
     return namedTypedQuery(WebhookMessageEntity.NEXT_TO_SEND)
             .setParameter("send_at", sendAt)
             .getResultList()
             .stream()
             .findAny();
    }
}
