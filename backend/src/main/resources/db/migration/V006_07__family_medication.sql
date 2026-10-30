-- =====================================================================
-- V006_07: Family sent medication: reconciliation before use and inventory tracking
-- Reference: data-model section 8; spec FR-040 -> FR-047, BR-M07-10, 11, 18, 19
-- =====================================================================

CREATE TABLE IF NOT EXISTS family_medication_batch (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resident_id BIGINT NOT NULL,
    medication_id BIGINT NULL,
    custom_name VARCHAR(255) NULL,
    custom_active_ingredient VARCHAR(255) NULL,
    custom_concentration VARCHAR(100) NULL,
    initial_quantity DECIMAL(10,2) NOT NULL,
    remaining_quantity DECIMAL(10,2) NOT NULL,
    unit VARCHAR(50) NOT NULL,
    expiry_date DATE NULL,
    packaging_condition VARCHAR(255) NULL,
    has_prescription TINYINT(1) NOT NULL DEFAULT 0,
    file_ids JSON NULL,
    deliverer_type ENUM('FAMILY_MEMBER', 'FREE', 'PURCHASING_STAFF') NOT NULL,
    deliverer_id BIGINT NULL,
    deliverer_name VARCHAR(255) NULL,
    relationship VARCHAR(100) NULL,
    purchase_request_id BIGINT NULL,
    receiver_id BIGINT NOT NULL,
    received_at TIMESTAMP(3) NOT NULL,
    status ENUM('PENDING_RECONCILIATION', 'IN_USE', 'HOLD_ONLY', 'DEPLETED', 'RETURNED', 'DISPOSED') NOT NULL DEFAULT 'PENDING_RECONCILIATION',
    status_reason VARCHAR(2000) NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT chk_batch_remaining_non_negative CHECK (remaining_quantity >= 0),
    INDEX idx_fmb_resident_status (resident_id, status),
    INDEX idx_fmb_status_expiry (status, expiry_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS family_medication_prescription (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    prescription_id BIGINT NOT NULL,
    bound_from TIMESTAMP(3) NOT NULL,
    bound_to TIMESTAMP(3) NULL,
    CONSTRAINT fk_fmp_batch FOREIGN KEY (batch_id) REFERENCES family_medication_batch (id) ON DELETE CASCADE,
    INDEX idx_fmp_batch (batch_id),
    INDEX idx_fmp_prescription (prescription_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS family_medication_movement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    movement_type ENUM('RECEIPT', 'DOSE_USAGE', 'PRN_USAGE', 'DOSE_CORRECTION', 'HANDOVER_LEAVE', 'RETURN_HANDOVER', 'INVENTORY_ADJUSTMENT', 'RETURN', 'DISPOSAL') NOT NULL,
    quantity DECIMAL(10,2) NOT NULL,
    remaining_after DECIMAL(10,2) NOT NULL,
    source_type VARCHAR(50) NULL,
    source_id BIGINT NULL,
    actor_id BIGINT NOT NULL,
    moved_at TIMESTAMP(3) NOT NULL,
    reason VARCHAR(2000) NULL,
    witness VARCHAR(255) NULL,
    recipient_name VARCHAR(255) NULL,
    relationship VARCHAR(100) NULL,
    CONSTRAINT fk_fmm_batch FOREIGN KEY (batch_id) REFERENCES family_medication_batch (id) ON DELETE CASCADE,
    INDEX idx_fmm_batch_time (batch_id, moved_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS family_medication_reminder (
    group_key VARCHAR(100) NOT NULL,
    milestone VARCHAR(50) NOT NULL,
    sequence_no INT NOT NULL,
    notified TINYINT(1) NOT NULL DEFAULT 0,
    days_calculated_last INT NULL,
    PRIMARY KEY (group_key, milestone, sequence_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
