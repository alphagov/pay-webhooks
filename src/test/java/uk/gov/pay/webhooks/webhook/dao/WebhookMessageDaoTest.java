package uk.gov.pay.webhooks.webhook.dao;

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
import java.time.InstantSource;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

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
    private InstantSource instantSource;

    @BeforeEach
    public void setUp() {
        webhookMessageDao = new WebhookMessageDao(database.getSessionFactory());
        instantSource = InstantSource.fixed(Instant.now());
        
    }

    @Test
    public void persistsSendAtDate() {

        WebhookMessageEntity persisted = database.inTransaction(() -> {
            WebhookMessageEntity webhookMessageEntity = new WebhookMessageEntity();
            webhookMessageEntity.setSendAt(Date.from(instantSource.instant()));
            return webhookMessageDao.create(webhookMessageEntity);
        });
        assertThat(persisted.getSendAt(), equalTo(Date.from(instantSource.instant())));
    }

    @Test
    public void getsEntitySendNext() {
        WebhookMessageEntity persisted = database.inTransaction(() -> {
            WebhookMessageEntity webhookMessageEntity = new WebhookMessageEntity();
            webhookMessageEntity.setSendAt(Date.from(instantSource.instant().minusMillis(1)));
            return webhookMessageDao.create(webhookMessageEntity);
        });

        Optional<WebhookMessageEntity> next = database.inTransaction(() -> 
                webhookMessageDao.nextToSend(Date.from(instantSource.instant())));
        
        assertThat(next, equalTo(Optional.of(persisted)));
    }
}
