CREATE TABLE password_reset_tokens
(
    id         UUID                           NOT NULL,
    user_id    UUID                           NOT NULL,
    token_hash VARCHAR(64)                    NOT NULL,
    expires_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    used_at    TIMESTAMP(6) WITHOUT TIME ZONE,
    created_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id)
);

CREATE INDEX idx_prt_expires ON password_reset_tokens (expires_at);

CREATE UNIQUE INDEX uq_prt_token_hash ON password_reset_tokens (token_hash);

ALTER TABLE password_reset_tokens
    ADD CONSTRAINT FK_PASSWORD_RESET_TOKENS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

CREATE INDEX idx_prt_user ON password_reset_tokens (user_id);