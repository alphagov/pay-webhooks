package uk.gov.pay.webhooks.util;

import org.jdbi.v3.core.Jdbi;
import uk.gov.pay.webhooks.deliveryqueue.DeliveryStatus;

import java.io.Serializable;
import java.util.List;

/*
  Group methods referencing same database tables together for ease of maintenance and future refactor .
  e.g. webhooks database table inserts are arranged at the top of the file.
 */
public class DatabaseTestHelper {
    private final Jdbi jdbi;

    private DatabaseTestHelper(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public static DatabaseTestHelper aDatabaseTestHelper(Jdbi jdbi) {
        return new DatabaseTestHelper(jdbi);
    }

    public void addWebhook(Webhook webhook) {
        jdbi.withHandle(h -> h.execute("INSERT INTO webhooks VALUES ('%d', '2022-01-01', '%s', 'signing-key', '%s', '%s', '%s', 'description', 'ACTIVE', '%s')"
                .formatted(webhook.getWebhookId(),
                        webhook.getWebhookExternalId(),
                        webhook.getServiceExternalId(),
                        webhook.getLive(),
                        webhook.getEndpointUrl(),
                        webhook.getGatewayAccountId())));
    }

    public void addWebhookSubscription(WebhookSubscription webhookSubscription) {
        jdbi.withHandle(h -> h.execute("INSERT INTO webhook_subscriptions VALUES ('%d', (SELECT id FROM event_types WHERE name = '%s'))"
                .formatted(webhookSubscription.getSubscriptionId(),
                        webhookSubscription.getEvent())));
    }

    public void addWebhookMessage(WebhookMessage webhookMessage) {
        jdbi.withHandle(h -> h.execute("""
                INSERT INTO webhook_messages VALUES
                ('%d', '%s', '%s', '%d', '%s', '%d', '%s', '%s', '%s', '%s')
                """.formatted(
                webhookMessage.getWebhookMessageId(),
                webhookMessage.getExternalId(),
                webhookMessage.getCreatedDate(),
                webhookMessage.getWebhookId(),
                webhookMessage.getEventDate(),
                webhookMessage.getEventType(),
                webhookMessage.getResource(),
                webhookMessage.getResourceExternalId(),
                webhookMessage.getResourceType(),
                webhookMessage.getDeliveryStatus())
        ));
    }

    public void addWebhookMessage(int startIdIndex, int recordCount, List<String> externalIdList, WebhookMessage webhookMessage) {
        for (int i = startIdIndex; i <= recordCount; i++) {
            webhookMessage.setWebhookMessageId(i);
            webhookMessage.setExternalId(externalIdList.get(i - 2));
            addWebhookMessage(webhookMessage);
        }
    }

    public void addWebhookDeliveryQueueMessage(WebhookDeliveryQueueMessage webhookDeliveryQueueMessage) {
        jdbi.withHandle(h -> h.execute("""
                INSERT INTO webhook_delivery_queue VALUES
                    ('%d', '%s', '%s', '%s', '%d', '%d', '%s', '%d')
                """.formatted(webhookDeliveryQueueMessage.getDeliveryQueueMessageId(),
                webhookDeliveryQueueMessage.getSentDate(),
                webhookDeliveryQueueMessage.getCreatedDate(),
                webhookDeliveryQueueMessage.getDeliveryResult(),
                webhookDeliveryQueueMessage.getStatusCode(),
                webhookDeliveryQueueMessage.getWebhookMessageId(),
                webhookDeliveryQueueMessage.getDeliveryStatus(),
                webhookDeliveryQueueMessage.getDeliveryCode())
        ));
    }

    public void addWebhookDeliveryQueueMessage(int startIdIndex, int recordCount, WebhookDeliveryQueueMessage webhookDeliveryQueueMessage) {
        for (int i = startIdIndex; i <= recordCount; i++) {
            webhookDeliveryQueueMessage.setDeliveryQueueMessageId(i);
            webhookDeliveryQueueMessage.setWebhookMessageId(i - 2);
            addWebhookDeliveryQueueMessage(webhookDeliveryQueueMessage);
        }
    }

    public void truncateAllWebhooksData() {
        jdbi.withHandle(h -> h.createScript(
                "TRUNCATE TABLE webhooks CASCADE; "
        ).execute());
    }

    public static class Webhook implements Serializable {
        private final int webhookId;
        private final String webhookExternalId;
        private final String serviceExternalId;
        private final String endpointUrl;
        private final String live;
        private final String gatewayAccountId;

        public Webhook(int webhookId, String webhookExternalId, String serviceExternalId, String endpointUrl, String live, String gatewayAccountId) {
            this.webhookId = webhookId;
            this.webhookExternalId = webhookExternalId;
            this.serviceExternalId = serviceExternalId;
            this.endpointUrl = endpointUrl;
            this.live = live;
            this.gatewayAccountId = gatewayAccountId;
        }

        public int getWebhookId() {
            return webhookId;
        }

        public String getWebhookExternalId() {
            return webhookExternalId;
        }

        public String getServiceExternalId() {
            return serviceExternalId;
        }

        public String getEndpointUrl() {
            return endpointUrl;
        }

        public String getLive() {
            return live;
        }

        public String getGatewayAccountId() {
            return gatewayAccountId;
        }
    }

    public static class WebhookSubscription implements Serializable {
        private final int subscriptionId;
        private final String event;

        public WebhookSubscription(int subscriptionId, String event) {
            this.subscriptionId = subscriptionId;
            this.event = event;
        }

        public int getSubscriptionId() {
            return subscriptionId;
        }

        public String getEvent() {
            return event;
        }
    }

    public static class WebhookMessage implements Serializable {
        private int webhookMessageId;
        private String externalId;
        private final String createdDate;
        private final int webhookId;
        private final String eventDate;
        private final int eventType;
        private final String resource;
        private final String resourceExternalId;
        private final String resourceType;
        private final DeliveryStatus deliveryStatus;

        public WebhookMessage(int webhookMessageId, String externalId, String createdDate, int webhookId, String eventDate, int eventType, String resource, String resourceExternalId, String resourceType, DeliveryStatus deliveryStatus) {
            this.webhookMessageId = webhookMessageId;
            this.externalId = externalId;
            this.createdDate = createdDate;
            this.webhookId = webhookId;
            this.eventDate = eventDate;
            this.eventType = eventType;
            this.resource = resource;
            this.resourceExternalId = resourceExternalId;
            this.resourceType = resourceType;
            this.deliveryStatus = deliveryStatus;
        }

        public void setWebhookMessageId(int webhookMessageId) {
            this.webhookMessageId = webhookMessageId;
        }

        public void setExternalId(String externalId) {
            this.externalId = externalId;
        }

        public int getWebhookMessageId() {
            return webhookMessageId;
        }

        public String getExternalId() {
            return externalId;
        }

        public String getCreatedDate() {
            return createdDate;
        }

        public int getWebhookId() {
            return webhookId;
        }

        public String getEventDate() {
            return eventDate;
        }

        public int getEventType() {
            return eventType;
        }

        public String getResource() {
            return resource;
        }

        public String getResourceExternalId() {
            return resourceExternalId;
        }

        public String getResourceType() {
            return resourceType;
        }

        public DeliveryStatus getDeliveryStatus() {
            return deliveryStatus;
        }
    }

    public static class WebhookDeliveryQueueMessage implements Serializable {
        private int deliveryQueueMessageId;
        private int webhookMessageId;
        private final String sentDate;
        private final String createdDate;
        private final String deliveryResult;
        private final int statusCode;
        private DeliveryStatus deliveryStatus;
        private int deliveryCode;

        public WebhookDeliveryQueueMessage(int deliveryQueueMessageId, int webhookMessageId, String sentDate, String createdDate, String deliveryResult, int statusCode, DeliveryStatus deliveryStatus, int deliveryCode) {
            this.deliveryQueueMessageId = deliveryQueueMessageId;
            this.webhookMessageId = webhookMessageId;
            this.sentDate = sentDate;
            this.createdDate = createdDate;
            this.deliveryResult = deliveryResult;
            this.statusCode = statusCode;
            this.deliveryStatus = deliveryStatus;
            this.deliveryCode = deliveryCode;
        }

        public int getDeliveryQueueMessageId() {
            return deliveryQueueMessageId;
        }

        public int getWebhookMessageId() {
            return webhookMessageId;
        }

        public String getSentDate() {
            return sentDate;
        }

        public String getCreatedDate() {
            return createdDate;
        }

        public String getDeliveryResult() {
            return deliveryResult;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public DeliveryStatus getDeliveryStatus() {
            return deliveryStatus;
        }

        public int getDeliveryCode() {
            return deliveryCode;
        }

        public void setDeliveryQueueMessageId(int deliveryQueueMessageId) {
            this.deliveryQueueMessageId = deliveryQueueMessageId;
        }

        public void setWebhookMessageId(int webhookMessageId) {
            this.webhookMessageId = webhookMessageId;
        }
    }
}
