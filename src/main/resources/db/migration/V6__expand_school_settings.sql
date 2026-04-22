-- ============================================================
-- V6: Expandir school_settings com abas Financeiro, Pedagogico,
-- Calendario e Portaria
-- ============================================================

ALTER TABLE school_settings
    -- === FINANCEIRO ===
    ADD COLUMN IF NOT EXISTS default_due_day INTEGER DEFAULT 10,
    ADD COLUMN IF NOT EXISTS default_payment_method VARCHAR(20) DEFAULT 'PIX',
    ADD COLUMN IF NOT EXISTS dunning_email_enabled BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS dunning_whatsapp_enabled BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS dunning_push_enabled BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS dunning_days_first INTEGER DEFAULT 3,
    ADD COLUMN IF NOT EXISTS dunning_days_second INTEGER DEFAULT 7,
    ADD COLUMN IF NOT EXISTS dunning_days_third INTEGER DEFAULT 15,

    -- === PEDAGOGICO ===
    ADD COLUMN IF NOT EXISTS grade_scale_type VARCHAR(30) DEFAULT 'NUMERIC_0_10',
    ADD COLUMN IF NOT EXISTS passing_grade NUMERIC(4, 2) DEFAULT 6.00,
    ADD COLUMN IF NOT EXISTS minimum_attendance_percent INTEGER DEFAULT 75,
    ADD COLUMN IF NOT EXISTS assessments_per_term INTEGER DEFAULT 2,

    -- === CALENDARIO ===
    ADD COLUMN IF NOT EXISTS academic_year_start DATE,
    ADD COLUMN IF NOT EXISTS academic_year_end DATE,
    ADD COLUMN IF NOT EXISTS minimum_school_days INTEGER DEFAULT 200,

    -- === PORTARIA ===
    ADD COLUMN IF NOT EXISTS qr_expiration_minutes INTEGER DEFAULT 30,
    ADD COLUMN IF NOT EXISTS gate_allowed_start_time TIME DEFAULT '06:00:00',
    ADD COLUMN IF NOT EXISTS gate_allowed_end_time TIME DEFAULT '19:00:00',
    ADD COLUMN IF NOT EXISTS gate_auto_approval BOOLEAN DEFAULT TRUE;

-- Comentarios explicativos
COMMENT ON COLUMN school_settings.default_due_day IS 'Dia do mes padrao para vencimento de faturas (1-31)';
COMMENT ON COLUMN school_settings.default_payment_method IS 'Metodo padrao: PIX, BOLETO, CARTAO';
COMMENT ON COLUMN school_settings.grade_scale_type IS 'NUMERIC_0_10, NUMERIC_0_100, LETTERS_A_F';
COMMENT ON COLUMN school_settings.minimum_school_days IS 'Dias letivos minimos exigidos (200 pela LDB)';
