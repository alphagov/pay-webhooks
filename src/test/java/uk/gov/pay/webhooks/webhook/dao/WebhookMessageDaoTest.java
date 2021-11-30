package uk.gov.pay.webhooks.webhook.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.testing.junit5.DAOTestExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeEntity;
import uk.gov.pay.webhooks.message.dao.WebhookMessageDao;
import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;

import java.sql.Date;
import java.time.Instant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(DropwizardExtensionsSupport.class)
public class WebhookMessageDaoTest {

    public DAOTestExtension database = DAOTestExtension.newBuilder()
            .addEntityClass(EventTypeEntity.class)
            .addEntityClass(WebhookMessageEntity.class)
            .addEntityClass(WebhookEntity.class)
            .build();

    private WebhookMessageDao webhookMessageDao;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        webhookMessageDao = new WebhookMessageDao(database.getSessionFactory());
        objectMapper = new ObjectMapper();
    }

    @Test
    public void findsMessageToSend() {

        WebhookMessageEntity persisted = database.inTransaction(() -> {
            WebhookMessageEntity webhookMessageEntity = new WebhookMessageEntity();
            webhookMessageEntity.setSendAt(Date.from(Instant.now()));
            webhookMessageEntity.setCreatedDate(Date.from(Instant.now()));
            webhookMessageEntity.setResource(objectMapper.createObjectNode());;
            return webhookMessageDao.create(webhookMessageEntity);
        });
        assertThat(persisted.getCreatedDate(), equalTo(Date.from(Instant.now())));
    }
}
