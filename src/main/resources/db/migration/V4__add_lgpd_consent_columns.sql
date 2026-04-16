-- LGPD: Add consent tracking to users
ALTER TABLE users ADD COLUMN IF NOT EXISTS lgpd_consent_at TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS lgpd_consent_ip VARCHAR(45);

-- LGPD: Add data deletion request tracking
CREATE TABLE IF NOT EXISTS lgpd_data_requests (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    school_id BIGINT,
    request_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    requested_at TIMESTAMP NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMP,
    processed_by VARCHAR(255),
    notes TEXT
);
