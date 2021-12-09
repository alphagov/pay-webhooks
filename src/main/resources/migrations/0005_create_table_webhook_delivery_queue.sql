CREATE table webhook_delivery_queue (
                                        id BIGSERIAL PRIMARY KEY,
                                        send_at DATE NOT NULL,
                                        created_date TIMESTAMP WITH TIME ZONE NOT NULL,
                                        delivery_result TEXT,
                                        status_code INT,
                                        webhook_message_id INT NOT NULL,
                                        delivery_status VARCHAR(64) NOT NULL check (delivery_status in ('SUCCESSFUL', 'FAILED', 'PENDING'))
);

ALTER TABLE webhook_delivery_queue ADD CONSTRAINT fk_webhook_message_id FOREIGN KEY (webhook_message_id) REFERENCES webhook_messages (id);
