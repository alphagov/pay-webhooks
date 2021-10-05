--liquibase formatted sql

--changeset uk.gov.pay:create-table-webhooks

CREATE table webhooks (
    id SERIAL PRIMARY KEY,
    created_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    external_id VARCHAR(30) NOT NULL,
    service_id VARCHAR(32) NOT NULL,
    live BOOLEAN NOT NULL,
    callback_url VARCHAR(2048) NOT NULL,
    description TEXT,
    status VARCHAR(32) NOT NULL
)

--rollback DROP table webhooks
