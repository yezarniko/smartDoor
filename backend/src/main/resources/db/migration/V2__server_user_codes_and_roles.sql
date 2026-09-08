CREATE TABLE roles (
    code VARCHAR(30) PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE,
    model_role VARCHAR(30) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT chk_role_code CHECK (code REGEXP '^[A-Z][A-Z0-9_]*$')
);

INSERT INTO roles (code, name, model_role, created_at, updated_at) VALUES
    ('ADMIN', 'Admin user', 'ADMIN', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('STAFF', 'Staff', 'STAFF', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('VISITOR', 'Visitor', 'VISITOR', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));

ALTER TABLE users ADD COLUMN user_code INT NULL UNIQUE AFTER public_id;

CREATE TEMPORARY TABLE user_code_migration (
    user_id CHAR(36) PRIMARY KEY,
    user_code INT NOT NULL UNIQUE
);

INSERT INTO user_code_migration (user_id, user_code)
SELECT id, ROW_NUMBER() OVER (ORDER BY created_at, id)
FROM users;

UPDATE users u JOIN user_code_migration m ON m.user_id = u.id
SET u.user_code = m.user_code;

DROP TEMPORARY TABLE user_code_migration;

ALTER TABLE users MODIFY user_code INT NOT NULL;
