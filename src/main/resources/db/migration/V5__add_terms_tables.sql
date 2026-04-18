-- Termos de Uso / LGPD: Versionamento e aceite de termos

CREATE TABLE terms_versions (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    version VARCHAR(20) NOT NULL,
    active BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255)
);

CREATE TABLE terms_acceptances (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT,
    user_id BIGINT NOT NULL,
    terms_version_id BIGINT NOT NULL REFERENCES terms_versions(id),
    accepted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent TEXT
);

CREATE INDEX idx_terms_acceptances_user ON terms_acceptances(user_id);
CREATE INDEX idx_terms_acceptances_school ON terms_acceptances(school_id);
CREATE INDEX idx_terms_versions_type_active ON terms_versions(type, active);
