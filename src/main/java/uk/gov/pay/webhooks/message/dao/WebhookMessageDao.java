package uk.gov.pay.webhooks.message.dao;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.LockOptions;
import org.hibernate.SessionFactory;
import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;

import javax.inject.Inject;
import javax.persistence.LockModeType;
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
    
    public Optional<WebhookMessageEntity> nextToSend(Date sendBefore){
     return namedTypedQuery(WebhookMessageEntity.NEXT_TO_SEND)
             .setParameter("send_before", sendBefore)
             .setMaxResults(1)
             .setLockMode(LockModeType.PESSIMISTIC_WRITE)
             .setHint(
                     "javax.persistence.lock.timeout",
                     LockOptions.SKIP_LOCKED
             )
             .getResultList()
             .stream()
             .findAny();
    }
}
