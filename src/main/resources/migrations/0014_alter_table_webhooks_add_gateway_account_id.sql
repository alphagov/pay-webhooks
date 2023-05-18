--liquibase formatted sql

--changeset uk.gov.pay:alter-table-webhooks-add-gateway-account-id
ALTER TABLE webhooks
ADD COLUMN gateway_account_id VARCHAR(255);

--changeset uk.gov.pay:add-index-webhooks-gateway-account-id runInTransaction:false
CREATE INDEX CONCURRENTLY IF NOT EXISTS gateway_account_id_idx on webhooks(gateway_account_id);