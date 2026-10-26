-- =====================================================================
-- V006_06: As-needed (PRN) medication with minimum interval and 24h rolling limit (FR-024, FR-025, FR-025a, BR-M07-04)
-- =====================================================================

CREATE TABLE IF NOT EXISTS prn_administration (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prescription_id BIGINT NOT NULL,
    resident_id BIGINT NOT NULL,
    administered_at TIMESTAMP(3) NOT NULL,
    dosage DECIMAL(12,3) NOT NULL,
    dosage_unit VARCHAR(30) NOT NULL,
    reason VARCHAR(2000) NOT NULL,
    administered_by_id BIGINT NOT NULL,
    reported_by VARCHAR(255) NULL,
    exceeded_limit TINYINT(1) NOT NULL DEFAULT 0,
    offline_device_time TIMESTAMP(3) NULL,
    family_medication_batch_id BIGINT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT chk_prn_dosage_positive CHECK (dosage > 0),
    INDEX idx_prn_prescription_time (prescription_id, administered_at),
    INDEX idx_prn_resident_time (resident_id, administered_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
