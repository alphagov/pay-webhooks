--liquibase formatted sql

--changeset uk.gov.pay:create-table-webhooks

CREATE TYPE live_or_test AS ENUM('LIVE', 'TEST');

CREATE table webhooks (
    id SERIAL PRIMARY KEY,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    external_id VARCHAR(30) NOT NULL,
    service_id VARCHAR(30) NOT NULL,
    type live_or_test NOT NULL,
    callback_url VARCHAR(2048) NOT NULL,
    description TEXT,
    status VARCHAR(32) NOT NULL
)

--rollback DROP table webhooks; DROP type live_or_test
