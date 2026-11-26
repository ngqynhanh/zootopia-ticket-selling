-- =====================================================================
-- Week 2: tables for authentication, audit trail and runtime settings.
-- =====================================================================

-- One-time / long-lived tokens. Only a SHA-256 hash is stored, never the token itself,
-- so a database leak does not give working tokens.
CREATE TABLE user_tokens (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)  NOT NULL UNIQUE,
    purpose     VARCHAR(30)  NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    -- Set when the token is consumed (verification, reset), rotated or revoked (refresh).
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_user_tokens_purpose CHECK (purpose IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET', 'REFRESH')),
    CONSTRAINT ck_user_tokens_expiry CHECK (expires_at > created_at)
);
CREATE INDEX ix_user_tokens_user_purpose ON user_tokens (user_id, purpose);
-- Speeds up "revoke all active refresh tokens of a user".
CREATE INDEX ix_user_tokens_active ON user_tokens (user_id) WHERE used_at IS NULL;

-- Who did what, when (login, role change, approval, price change...).
CREATE TABLE audit_logs (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    actor_id     BIGINT REFERENCES users (id) ON DELETE SET NULL,
    actor_email  VARCHAR(255),
    action       VARCHAR(40)  NOT NULL,
    entity_type  VARCHAR(40)  NOT NULL,
    entity_id    BIGINT,
    old_value    JSONB,
    new_value    JSONB,
    ip_address   VARCHAR(45),
    request_id   VARCHAR(64),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_audit_logs_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX ix_audit_logs_actor_created ON audit_logs (actor_id, created_at DESC);
CREATE INDEX ix_audit_logs_action_created ON audit_logs (action, created_at DESC);
CREATE INDEX ix_audit_logs_created ON audit_logs (created_at DESC);

-- Business parameters an admin can change without redeploying.
CREATE TABLE system_settings (
    setting_key    VARCHAR(60)  PRIMARY KEY,
    setting_value  VARCHAR(500) NOT NULL,
    value_type     VARCHAR(10)  NOT NULL DEFAULT 'STRING',
    description    VARCHAR(300),
    is_public      BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by     BIGINT REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_system_settings_type CHECK (value_type IN ('STRING', 'INTEGER', 'DECIMAL', 'BOOLEAN', 'TIME'))
);
CREATE INDEX ix_system_settings_updated_by ON system_settings (updated_by);

CREATE TRIGGER trg_system_settings_updated_at
    BEFORE UPDATE ON system_settings FOR EACH ROW EXECUTE FUNCTION set_updated_at();
