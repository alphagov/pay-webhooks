--liquibase formatted sql

--changeset uk.gov.pay:alter-table-webhook-messages-add-index-on-resource-external-id
CREATE INDEX IF NOT EXISTS webhook_messages_resource_external_id_idx on webhook_messages(resource_external_id)

--rollback DROP INDEX IF EXISTS webhook_messages_resource_external_id_idx
