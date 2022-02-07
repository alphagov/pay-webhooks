--liquibase formatted sql

--changeset uk.gov.pay:alter-table-webhooks-alter-column-created_date-type-timestamp-with-time-zone.sql
ALTER TABLE webhooks ALTER COLUMN created_date TYPE TIMESTAMP WITH TIME ZONE

--rollback ALTER TABLE webhooks ALTER COLUMN created_date TYPE TIMESTAMP WITHOUT TIME ZONE
