CREATE TYPE notification_channel AS ENUM (
    'EMAIL',
    'SMS',
    'PUSH',
    'WHATSAPP'
);

CREATE TYPE notification_status AS ENUM (
    'PENDING',
    'PROCESSING',
    'DELIVERED',
    'RETRYING',
    'FAILED',
    'DEAD_LETTER'
);

CREATE TYPE notification_priority AS ENUM (
    'HIGH',
    'NORMAL',
    'LOW'
);

CREATE TYPE notification_attempt_status AS ENUM (
    'STARTED',
    'DELIVERED',
    'FAILED'
);


CREATE TABLE notifications (

    id UUID PRIMARY KEY,

    channel notification_channel NOT NULL,

    recipient VARCHAR(255) NOT NULL
        CHECK (length(trim(recipient)) > 0),

    subject VARCHAR(255),

    content TEXT NOT NULL
        CHECK (length(trim(content)) > 0),

    status notification_status NOT NULL
        DEFAULT 'PENDING',

    priority notification_priority NOT NULL
        DEFAULT 'NORMAL',

    attempts INT NOT NULL
        DEFAULT 0
        CHECK (attempts >= 0),

    idempotency_key VARCHAR(255) UNIQUE,

    created_at TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    processed_at TIMESTAMPTZ,

    last_error TEXT
);


CREATE INDEX idx_notifications_status
    ON notifications(status);


CREATE TABLE notification_attempts (

    id BIGSERIAL PRIMARY KEY,

    notification_id UUID NOT NULL
        REFERENCES notifications(id)
        ON DELETE CASCADE,

    attempt_number INT NOT NULL
        CHECK (attempt_number > 0),

    status notification_attempt_status NOT NULL,

    error_message TEXT,

    started_at TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    finished_at TIMESTAMPTZ,

    CONSTRAINT uq_attempt_notification_number
        UNIQUE (notification_id, attempt_number)
);


CREATE INDEX idx_attempts_notification_id
    ON notification_attempts(notification_id);