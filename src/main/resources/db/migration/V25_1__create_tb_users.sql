-- Create TB_USERS so Flyway data migrations (V26, V27) can run.
-- Previously TB_USERS was created by Hibernate (init-db profile) only; fresh DBs (e.g. local Docker) had no table.
-- Schema matches User entity and V24 (ROLE column).

CREATE TABLE IF NOT EXISTS TB_USERS (
    TB_USERS_UID VARCHAR(36) NOT NULL PRIMARY KEY,
    USERNAME     VARCHAR(50) NOT NULL,
    PASSWORD_HASH VARCHAR(255) NOT NULL,
    CREATED_AT   TIMESTAMP(6) NOT NULL,
    UPDATED_AT   TIMESTAMP(6),
    LAST_LOGIN_AT TIMESTAMP(6),
    ROLE         VARCHAR(20) NOT NULL DEFAULT 'User'
);

CREATE UNIQUE INDEX IF NOT EXISTS IDX_USER_USERNAME ON TB_USERS (USERNAME);

COMMENT ON TABLE TB_USERS IS '사용자 (인증/역할)';
COMMENT ON COLUMN TB_USERS.ROLE IS '역할: User, Admin';
