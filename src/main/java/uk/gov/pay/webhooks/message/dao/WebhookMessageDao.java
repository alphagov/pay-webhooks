package uk.gov.pay.webhooks.message.dao;

import io.dropwizard.hibernate.AbstractDAO;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;
import uk.gov.pay.webhooks.webhook.resource.WebhookMessageSearchParams;

import jakarta.inject.Inject;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public class WebhookMessageDao extends AbstractDAO<WebhookMessageEntity> {

    public static final int WEBHOOK_MESSAGES_PAGE_SIZE = 10;
    
    private static final String SEARCH_WEBHOOK_MESSAGES = 
            "SELECT m from WebhookMessageEntity m " +
                    " WHERE webhookEntity = :webhook " +
                    " :searchExtraFields " +
                    " ORDER BY createdDate desc";

    @Inject
    public WebhookMessageDao(SessionFactory factory) {
        super(factory);
    }

    public WebhookMessageEntity create(WebhookMessageEntity webhookMessage) {
        persist(webhookMessage);
        return webhookMessage;
    }

    public Optional<WebhookMessageEntity> get(WebhookEntity webhook, String messageId) {
        return namedTypedQuery(WebhookMessageEntity.MESSAGE_BY_WEBHOOK_ID_AND_MESSAGE_ID)
                .setParameter("webhook", webhook)
                .setParameter("messageId", messageId)
                .stream()
                .findFirst();
    }
    
    public long getTotalMessagesCount(WebhookEntity webhook, WebhookMessageSearchParams params) {
        Query<?> query;
        if (params.getDeliveryStatus() == null) {
            query = namedQuery(WebhookMessageEntity.COUNT_MESSAGES_BY_WEBHOOK_ID)
                    .setParameter("webhook", webhook);
        } else {
            query = namedQuery(WebhookMessageEntity.COUNT_MESSAGES_BY_WEBHOOK_ID_AND_STATUS)
                    .setParameter("webhook", webhook)
                    .setParameter("deliveryStatus", params.getDeliveryStatus());
        }

        return (Long) query.uniqueResult();
    }

    public List<WebhookMessageEntity> list(WebhookEntity webhook, WebhookMessageSearchParams params) {
        String searchClauseTemplate = String.join(" AND ", params.getFilterTemplates());
        searchClauseTemplate = StringUtils.isNotBlank(searchClauseTemplate) ?  "AND " + searchClauseTemplate : "";

        String queryTemplate = SEARCH_WEBHOOK_MESSAGES.replace(":searchExtraFields", searchClauseTemplate);
        Query<WebhookMessageEntity> query = query(queryTemplate);

        params.getQueryMap().forEach(query::setParameter);
        query.setParameter("webhook", webhook);
        
        return query.setFirstResult(calculateFirstResult(params.getPage()))
                .setMaxResults(WEBHOOK_MESSAGES_PAGE_SIZE)
                .getResultList();
    }

    private int calculateFirstResult(int page) {
        return (page - 1) * WEBHOOK_MESSAGES_PAGE_SIZE;
    }

    public int deleteMessages(int days, int maxNumOfMessagesToDelete) {
        return currentSession().createNativeQuery("delete from webhook_messages where id in " +
                        "(select id from webhook_messages where created_date < :datetime limit :maxNumOfMessagesToDelete)")
                .setParameter("datetime", OffsetDateTime.now().minusDays(days))
                .setParameter("maxNumOfMessagesToDelete", maxNumOfMessagesToDelete)
                .executeUpdate();
    }
}
