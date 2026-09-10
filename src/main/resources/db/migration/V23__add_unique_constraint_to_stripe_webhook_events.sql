ALTER TABLE stripe_webhook_events
    ADD CONSTRAINT uk_webhook_event_id UNIQUE (event_id);