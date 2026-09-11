CREATE TABLE email_verification_tokens
(
    id         UUID                           NOT NULL,
    user_id    UUID                           NOT NULL,
    token_hash VARCHAR(64)                    NOT NULL,
    expires_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    used_at    TIMESTAMP(6) WITHOUT TIME ZONE,
    created_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_email_verification_tokens PRIMARY KEY (id)
);

CREATE INDEX idx_evt_expires ON email_verification_tokens (expires_at);

CREATE UNIQUE INDEX uq_evt_token_hash ON email_verification_tokens (token_hash);

ALTER TABLE email_verification_tokens
    ADD CONSTRAINT FK_EMAIL_VERIFICATION_TOKENS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

CREATE INDEX idx_evt_user ON email_verification_tokens (user_id);