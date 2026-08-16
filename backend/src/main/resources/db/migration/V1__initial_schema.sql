CREATE TABLE administrators (
    id CHAR(36) PRIMARY KEY,
    username VARCHAR(80) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL
);

CREATE TABLE users (
    id CHAR(36) PRIMARY KEY,
    public_id VARCHAR(40) NOT NULL UNIQUE,
    full_name VARCHAR(160) NOT NULL,
    email VARCHAR(190) NULL,
    phone VARCHAR(50) NULL,
    role VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    INDEX idx_users_status (status),
    INDEX idx_users_name (full_name)
);

CREATE TABLE doors (
    id CHAR(36) PRIMARY KEY,
    public_id VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL
);

CREATE TABLE user_door_permissions (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    door_id CHAR(36) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_permission_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_permission_door FOREIGN KEY (door_id) REFERENCES doors(id),
    CONSTRAINT uq_permission UNIQUE (user_id, door_id)
);

CREATE TABLE access_schedules (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    day_of_week VARCHAR(12) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_schedule_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_schedule_time CHECK (start_time < end_time),
    INDEX idx_schedule_user_day (user_id, day_of_week)
);

CREATE TABLE qr_credentials (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    nonce_hash CHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL,
    usage_mode VARCHAR(30) NOT NULL,
    usage_count INT NOT NULL DEFAULT 0,
    max_uses INT NULL,
    issued_at TIMESTAMP(6) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    revoked_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_credential_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_credential_user (user_id),
    INDEX idx_credential_status_expiry (status, expires_at)
);

CREATE TABLE devices (
    id CHAR(36) PRIMARY KEY,
    public_id VARCHAR(40) NOT NULL UNIQUE,
    door_id CHAR(36) NOT NULL,
    type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    last_heartbeat_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_device_door FOREIGN KEY (door_id) REFERENCES doors(id),
    INDEX idx_device_door (door_id)
);

CREATE TABLE access_events (
    id CHAR(36) PRIMARY KEY,
    request_id CHAR(36) NOT NULL UNIQUE,
    user_id CHAR(36) NULL,
    credential_id CHAR(36) NULL,
    door_id CHAR(36) NULL,
    device_id CHAR(36) NULL,
    result_code VARCHAR(50) NOT NULL,
    execution_status VARCHAR(40) NOT NULL,
    model_result VARCHAR(30) NULL,
    model_version VARCHAR(80) NULL,
    evaluation_path TEXT NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_event_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_event_credential FOREIGN KEY (credential_id) REFERENCES qr_credentials(id),
    CONSTRAINT fk_event_door FOREIGN KEY (door_id) REFERENCES doors(id),
    CONSTRAINT fk_event_device FOREIGN KEY (device_id) REFERENCES devices(id),
    INDEX idx_event_time (occurred_at),
    INDEX idx_event_result (result_code)
);

CREATE TABLE device_events (
    id CHAR(36) PRIMARY KEY,
    device_id CHAR(36) NOT NULL,
    request_id CHAR(36) NULL,
    event_type VARCHAR(50) NOT NULL,
    payload TEXT NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_device_event_device FOREIGN KEY (device_id) REFERENCES devices(id),
    INDEX idx_device_event_time (occurred_at)
);
