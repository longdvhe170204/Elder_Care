-- =====================================================================
-- V006_08: Supplied medication intake (FR-054, FR-055, BR-M07-24, Q-337)
-- =====================================================================

CREATE TABLE IF NOT EXISTS supplied_medication_intake (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    procurement_line_id BIGINT NOT NULL,
    procurement_request_id BIGINT NOT NULL,
    resident_id BIGINT NOT NULL,
    prescription_id BIGINT NOT NULL,
    medication_name VARCHAR(255) NOT NULL,
    quantity_received DECIMAL(10,2) NOT NULL,
    unit VARCHAR(50) NOT NULL,
    intake_result ENUM('PASS', 'FAIL') NOT NULL,
    discrepancy VARCHAR(2000) NULL,
    reason VARCHAR(2000) NULL,
    receiver_id BIGINT NOT NULL,
    received_at TIMESTAMP(3) NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT chk_intake_quantity_positive CHECK (quantity_received > 0),
    INDEX idx_smi_request (procurement_request_id),
    INDEX idx_smi_line (procurement_line_id),
    INDEX idx_smi_resident (resident_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
