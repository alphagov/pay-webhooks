--liquibase formatted sql

--changeset uk.gov.pay:create-table-webhook_messages

CREATE table webhook_messages (
    id BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(30) NOT NULL,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL,
    webhook_id INT NOT NULL,
    event_date TIMESTAMP WITH TIME ZONE NOT NULL,
    event_type INT NOT NULL,
    resource JSON NOT NULL,
    send_at TIMESTAMP WITH TIME ZONE
);

ALTER TABLE webhook_messages ADD CONSTRAINT fk_webhook_message_webhook_id FOREIGN KEY (webhook_id) REFERENCES webhooks (id);
ALTER TABLE webhook_messages ADD CONSTRAINT fk_webhook_message_event_type_id FOREIGN KEY (event_type) REFERENCES event_types (id);

--rollback DROP table webhook_messages
