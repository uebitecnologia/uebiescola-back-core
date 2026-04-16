-- UebiEscola Core Service - Baseline Migration
-- Gerado automaticamente a partir das entidades JPA

CREATE TABLE IF NOT EXISTS schools (
    id BIGSERIAL PRIMARY KEY,
    external_id UUID NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    legal_name VARCHAR(255) NOT NULL,
    cnpj VARCHAR(255) NOT NULL UNIQUE,
    state_registration VARCHAR(255) UNIQUE,
    subdomain VARCHAR(255) UNIQUE,
    logo_bytes BYTEA,
    logo_content_type VARCHAR(255),
    primary_color VARCHAR(255),
    pix_key VARCHAR(255),
    late_fee_percentage DOUBLE PRECISION,
    interest_rate DOUBLE PRECISION,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    admin_user_id BIGINT
);

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    external_id UUID NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    cpf VARCHAR(14) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50),
    school_id BIGINT,
    active BOOLEAN DEFAULT true,
    access_level_id BIGINT
);

CREATE TABLE IF NOT EXISTS school_addresses (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL,
    zip_code VARCHAR(255),
    street VARCHAR(255),
    complement VARCHAR(255),
    number VARCHAR(255),
    neighborhood VARCHAR(255),
    city VARCHAR(255),
    state VARCHAR(255),
    phone VARCHAR(255),
    mobile VARCHAR(255),
    CONSTRAINT fk_school_address_school FOREIGN KEY (school_id) REFERENCES schools(id)
);

CREATE TABLE IF NOT EXISTS school_settings (
    school_id BIGINT PRIMARY KEY,
    two_factor_enabled BOOLEAN DEFAULT false,
    notify_enrollment BOOLEAN DEFAULT true,
    notify_delinquency BOOLEAN DEFAULT false,
    notify_exam_reminder BOOLEAN DEFAULT true,
    backup_schedule VARCHAR(255) DEFAULT 'DAILY_04',
    api_key VARCHAR(255),
    CONSTRAINT fk_school_settings_school FOREIGN KEY (school_id) REFERENCES schools(id)
);

CREATE TABLE IF NOT EXISTS school_contracts (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL,
    plan_base VARCHAR(255),
    monthly_value DECIMAL(19,2),
    setup_value DECIMAL(19,2),
    expiration_day INTEGER,
    start_date DATE,
    CONSTRAINT fk_school_contract_school FOREIGN KEY (school_id) REFERENCES schools(id)
);

CREATE TABLE IF NOT EXISTS school_contract_modules (
    contract_id BIGINT NOT NULL,
    module_name VARCHAR(255),
    CONSTRAINT fk_contract_modules FOREIGN KEY (contract_id) REFERENCES school_contracts(id)
);

CREATE TABLE IF NOT EXISTS access_levels (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    permissions TEXT,
    active BOOLEAN DEFAULT true,
    system_default BOOLEAN DEFAULT false
);

CREATE TABLE IF NOT EXISTS global_settings (
    id BIGSERIAL PRIMARY KEY,
    setting_key VARCHAR(255) NOT NULL UNIQUE,
    setting_value TEXT,
    category VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL,
    user_email VARCHAR(255),
    action VARCHAR(255) NOT NULL,
    details VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
