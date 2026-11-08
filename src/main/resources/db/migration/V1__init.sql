-- Initial schema. Mirrors the aggregates in domain/: users, roles,
-- permissions (+ join tables), refresh / password-reset / email-verification
-- tokens, file assets, notifications and the audit log.

CREATE TABLE users (
    id                UUID PRIMARY KEY,
    name              VARCHAR(120) NOT NULL,
    email             VARCHAR(255) NOT NULL,
    password_hash     VARCHAR(255) NOT NULL,
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    email_verified_at TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at        TIMESTAMPTZ,
    created_by        UUID REFERENCES users (id) ON DELETE SET NULL,
    updated_by        UUID REFERENCES users (id) ON DELETE SET NULL
);

CREATE UNIQUE INDEX users_email_key ON users (email);
CREATE INDEX users_deleted_at_idx ON users (deleted_at);
CREATE INDEX users_created_by_idx ON users (created_by);
CREATE INDEX users_updated_by_idx ON users (updated_by);

CREATE TABLE roles (
    id          UUID PRIMARY KEY,
    name        VARCHAR(60) NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ
);

CREATE UNIQUE INDEX roles_name_key ON roles (name);
CREATE INDEX roles_deleted_at_idx ON roles (deleted_at);

CREATE TABLE permissions (
    id          UUID PRIMARY KEY,
    name        VARCHAR(60) NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ
);

CREATE UNIQUE INDEX permissions_name_key ON permissions (name);
CREATE INDEX permissions_deleted_at_idx ON permissions (deleted_at);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX user_roles_role_id_idx ON user_roles (role_id);

CREATE TABLE role_permissions (
    role_id       UUID NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions (id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE INDEX role_permissions_permission_id_idx ON role_permissions (permission_id);

CREATE TABLE refresh_tokens (
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX refresh_tokens_token_hash_key ON refresh_tokens (token_hash);
CREATE INDEX refresh_tokens_user_id_idx ON refresh_tokens (user_id);

CREATE TABLE password_reset_tokens (
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX password_reset_tokens_token_hash_key ON password_reset_tokens (token_hash);
CREATE INDEX password_reset_tokens_user_id_idx ON password_reset_tokens (user_id);

CREATE TABLE email_verification_tokens (
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX email_verification_tokens_token_hash_key ON email_verification_tokens (token_hash);
CREATE INDEX email_verification_tokens_user_id_idx ON email_verification_tokens (user_id);

CREATE TABLE file_assets (
    id          UUID PRIMARY KEY,
    file_name   VARCHAR(255) NOT NULL,
    mime_type   VARCHAR(120) NOT NULL,
    size        BIGINT       NOT NULL,
    storage_key VARCHAR(320) NOT NULL,
    uploaded_by UUID REFERENCES users (id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ
);

CREATE UNIQUE INDEX file_assets_storage_key_key ON file_assets (storage_key);
CREATE INDEX file_assets_uploaded_by_idx ON file_assets (uploaded_by);

CREATE TABLE notifications (
    id         UUID PRIMARY KEY,
    user_id    UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type       VARCHAR(60)  NOT NULL,
    title      VARCHAR(255) NOT NULL,
    message    TEXT         NOT NULL,
    read_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX notifications_user_id_idx ON notifications (user_id);

CREATE TABLE audit_logs (
    id            UUID PRIMARY KEY,
    actor_id      UUID REFERENCES users (id) ON DELETE SET NULL,
    action        VARCHAR(120) NOT NULL,
    resource_type VARCHAR(60)  NOT NULL,
    resource_id   VARCHAR(64),
    metadata      JSONB,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX audit_logs_actor_id_idx ON audit_logs (actor_id);
CREATE INDEX audit_logs_resource_idx ON audit_logs (resource_type, resource_id);
