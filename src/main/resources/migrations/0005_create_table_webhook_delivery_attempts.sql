--liquibase formatted sql

--changeset uk.gov.pay:create-table-webhook_delivery_attempts

CREATE table webhook_delivery_attempts (
                                  id BIGSERIAL PRIMARY KEY,
                                  created_date TIMESTAMP WITH TIME ZONE NOT NULL,
                                  webhook_id INT NOT NULL,
                                  delivery_status TEXT,
                                  webhook_message_id INT NOT NULL
);

ALTER TABLE webhook_delivery_attempts ADD CONSTRAINT fk_webhook_id FOREIGN KEY (webhook_id) REFERENCES webhooks (id);
ALTER TABLE webhook_delivery_attempts ADD CONSTRAINT fk_webhook_message_id FOREIGN KEY (webhook_message_id) REFERENCES webhook_messages (id);

--rollback DROP table webhook_messages
