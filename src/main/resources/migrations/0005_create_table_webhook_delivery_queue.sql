--liquibase formatted sql

--changeset uk.gov.pay:create-table-webhook_delivery_queue

CREATE table webhook_delivery_queue (
                                  id BIGSERIAL PRIMARY KEY,
                                  send_at DATE,
                                  created_date TIMESTAMP WITH TIME ZONE NOT NULL,
                                  delivery_text TEXT,
                                  webhook_message_id INT NOT NULL,
                                  delivery_status VARCHAR(64)
);

ALTER TABLE webhook_delivery_queue ADD CONSTRAINT fk_webhook_id FOREIGN KEY (webhook_id) REFERENCES webhooks (id);
ALTER TABLE webhook_delivery_queue ADD CONSTRAINT fk_webhook_message_id FOREIGN KEY (webhook_message_id) REFERENCES webhook_messages (id);

--rollback DROP table webhook_delivery_queue
