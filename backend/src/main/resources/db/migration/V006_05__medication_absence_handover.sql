-- =====================================================================
-- V006_05: Absence intervals and medication handover/return (FR-021, FR-048, FR-048a, FR-048b, BR-M07-03)
-- =====================================================================

-- 1. Resident absence intervals affecting medication administration
CREATE TABLE IF NOT EXISTS medication_absence_interval (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resident_id BIGINT NOT NULL,
    absence_type ENUM(
        'TEMPORARY_LEAVE_NO_MED',
        'TEMPORARY_LEAVE_WITH_MED',
        'HOSPITAL',
        'TRIP_WITH_MED',
        'TRIP_NO_MED'
    ) NOT NULL,
    from_time TIMESTAMP(3) NOT NULL,
    expected_to_time TIMESTAMP(3) NULL,
    actual_to_time TIMESTAMP(3) NULL,
    absence_session_id BIGINT NULL,
    trip_id BIGINT NULL,
    no_medication_order_id BIGINT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    -- Generated column: At most one open absence interval per resident (actual_to_time IS NULL)
    is_open TINYINT(1) GENERATED ALWAYS AS (IF(actual_to_time IS NULL, 1, NULL)) VIRTUAL,
    CONSTRAINT uq_open_absence_resident UNIQUE (resident_id, is_open),
    INDEX idx_absence_resident_time (resident_id, from_time, expected_to_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Medication handover table
CREATE TABLE IF NOT EXISTS medication_handover (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resident_id BIGINT NOT NULL,
    absence_interval_id BIGINT NULL,
    trip_id BIGINT NULL,
    recipient_type ENUM('STAFF', 'TRIP_LEADER', 'RELATIVE') NOT NULL,
    recipient_id BIGINT NULL,
    recipient_name VARCHAR(255) NULL,
    relationship VARCHAR(100) NULL,
    dispensed_by_id BIGINT NOT NULL,
    handover_time TIMESTAMP(3) NOT NULL,
    status ENUM('HANDED_OVER', 'WAITING_RETURN', 'RETURNED') NOT NULL DEFAULT 'HANDED_OVER',
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_handover_resident (resident_id),
    INDEX idx_handover_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Medication handover items table
CREATE TABLE IF NOT EXISTS medication_handover_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    handover_id BIGINT NOT NULL,
    prescription_id BIGINT NOT NULL,
    quantity DECIMAL(12,3) NOT NULL,
    family_medication_batch_id BIGINT NULL,
    medication_source VARCHAR(30) NULL,
    CONSTRAINT fk_handover_item_handover FOREIGN KEY (handover_id) REFERENCES medication_handover(id) ON DELETE CASCADE,
    INDEX idx_handover_item_prescription (prescription_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Medication handover dose link table
CREATE TABLE IF NOT EXISTS medication_handover_dose (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    handover_id BIGINT NOT NULL,
    dose_id BIGINT NOT NULL UNIQUE,
    reverted_at TIMESTAMP(3) NULL,
    CONSTRAINT fk_handover_dose_handover FOREIGN KEY (handover_id) REFERENCES medication_handover(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Medication return table
CREATE TABLE IF NOT EXISTS medication_return (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    handover_id BIGINT NOT NULL UNIQUE,
    recorded_by_id BIGINT NOT NULL,
    return_time TIMESTAMP(3) NOT NULL,
    reason VARCHAR(2000) NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_med_return_handover FOREIGN KEY (handover_id) REFERENCES medication_handover(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Medication return items table
CREATE TABLE IF NOT EXISTS medication_return_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    return_id BIGINT NOT NULL,
    prescription_id BIGINT NOT NULL,
    quantity DECIMAL(12,3) NOT NULL,
    CONSTRAINT chk_return_qty_non_negative CHECK (quantity >= 0),
    CONSTRAINT fk_return_item_return FOREIGN KEY (return_id) REFERENCES medication_return(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. No medication for trip order table
CREATE TABLE IF NOT EXISTS no_medication_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resident_id BIGINT NOT NULL,
    trip_id BIGINT NOT NULL,
    reason VARCHAR(2000) NOT NULL,
    created_by_id BIGINT NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT uq_resident_trip_no_med UNIQUE (resident_id, trip_id),
    INDEX idx_no_med_resident (resident_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
