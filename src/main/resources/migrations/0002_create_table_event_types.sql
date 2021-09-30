--liquibase formatted sql

--changeset uk.gov.pay:create-table-event_types

CREATE table event_types (
    id SERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL
);

INSERT INTO event_types(name) VALUES (('card_payment_captured'));

--rollback DROP table event_types
