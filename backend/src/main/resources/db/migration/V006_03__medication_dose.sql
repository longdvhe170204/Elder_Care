-- 006 T022: Medication dose tables for Feature 006 (Module M07) - Medication Management.
-- Tables: medication_dose (Group 2), dose_result (Group 3 append-only), dose_review (Group 2).
-- Reference: data-model section 5; research D-06, D-07, D-08, D-09, D-11; spec FR-018 -> FR-023, FR-026 -> FR-033, BR-M07-01, BR-M07-02, BR-M07-12, BR-M07-13, DBR-13.

-- 1. medication_dose - Group 2 (Materialized medication dose records)
CREATE TABLE medication_dose
(
    id                            BIGINT                                                                                  NOT NULL AUTO_INCREMENT PRIMARY KEY,
    prescription_id               BIGINT                                                                                  NOT NULL COMMENT 'Prescription ID (DBR-13)',
    resident_id                   BIGINT                                                                                  NOT NULL COMMENT 'Resident logic ID',
    medication_id                 BIGINT                                                                                  NOT NULL COMMENT 'Medication catalog ID copied for reporting',
    scheduled_time                DATETIME(6)                                                                             NOT NULL COMMENT 'Target scheduled administration time',
    window_from                   DATETIME(6)                                                                             NOT NULL COMMENT 'Beginning of allowable administration window',
    window_to                     DATETIME(6)                                                                             NOT NULL COMMENT 'End of allowable administration window',
    dosage                        DECIMAL(10, 2)                                                                          NOT NULL COMMENT 'Prescribed dose amount',
    dosage_unit                   VARCHAR(50)                                                                             NOT NULL COMMENT 'Dosage unit (pill, ml, etc.)',
    medication_source             ENUM ('FACILITY', 'FAMILY_SENT')                                                        NOT NULL COMMENT 'Source of medication',
    special_control               BOOLEAN                                                                                 NOT NULL DEFAULT FALSE COMMENT 'Special control flag copied from catalog',
    status                        ENUM ('UPCOMING', 'DUE', 'LATE', 'MISSED', 'ADMINISTERED', 'REFUSED', 'NOT_ADMINISTERED', 'SUSPENDED', 'LEAVE_MEDICATION', 'CANCELLED') NOT NULL DEFAULT 'UPCOMING' COMMENT 'Dose lifecycle status',
    due_at                        DATETIME(6)                                                                             NULL COMMENT 'Timestamp when dose transitioned to DUE',
    late_at                       DATETIME(6)                                                                             NULL COMMENT 'Timestamp when dose transitioned to LATE',
    closed_at                     DATETIME(6)                                                                             NULL COMMENT 'Timestamp when dose transitioned to terminal status',
    ended_at                      DATETIME(6)                                                                             NULL COMMENT 'Timestamp when SUSPENDED dose passed window_to and locked',
    cancellation_reason           ENUM ('DISCONTINUED', 'REPLACED', 'EXPIRED', 'RECONCILIATION', 'FINAL_STATE')           NULL COMMENT 'Cancellation cause if CANCELLED',
    closed_by_type                ENUM ('EMPLOYEE', 'SYSTEM')                                                             NULL COMMENT 'Actor type that closed the dose',
    closed_by_id                  BIGINT                                                                                  NULL COMMENT 'Account ID of closer or NULL if SYSTEM',
    closure_rule                  VARCHAR(50)                                                                             NULL COMMENT 'Rule identifier for closure (e.g. BR_M07_02)',
    pending_post_return_recording BOOLEAN                                                                                 NOT NULL DEFAULT FALSE COMMENT 'Flag for leave doses awaiting confirmation after return',
    late_reminded_at              DATETIME(6)                                                                             NULL COMMENT 'Timestamp when late reminder notification was sent',
    floor_area_id                 BIGINT                                                                                  NULL COMMENT 'Resident floor/area at dose closure',
    version                       INT                                                                                     NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
    created_at                    DATETIME(6)                                                                             NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                    DATETIME(6)                                                                             NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_medication_dose_prescription FOREIGN KEY (prescription_id) REFERENCES prescription (id) ON DELETE RESTRICT,
    CONSTRAINT fk_medication_dose_medication FOREIGN KEY (medication_id) REFERENCES medication (id) ON DELETE RESTRICT,
    CONSTRAINT uk_medication_dose_prescription_scheduled UNIQUE (prescription_id, scheduled_time),
    KEY idx_medication_dose_status_window_from (status, window_from),
    KEY idx_medication_dose_status_window_to (status, window_to),
    KEY idx_medication_dose_status_late (status, late_at),
    KEY idx_medication_dose_resident_scheduled (resident_id, scheduled_time),
    KEY idx_medication_dose_resident_med_scheduled (resident_id, medication_id, scheduled_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- 2. dose_result - Group 3 append-only (Result snapshot of a closed dose)
CREATE TABLE dose_result
(
    id                           BIGINT                                                                                                   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    dose_id                      BIGINT                                                                                                   NOT NULL UNIQUE COMMENT 'Single closure record per dose (DBR-13, BR-M07-12)',
    resident_id                  BIGINT                                                                                                   NOT NULL COMMENT 'Resident logic ID',
    floor_area_id                BIGINT                                                                                                   NULL COMMENT 'Floor area at recording',
    result_type                  ENUM ('ADMINISTERED', 'REFUSED', 'NOT_ADMINISTERED', 'MISSED')                                           NOT NULL COMMENT 'Outcome type',
    administered_at              DATETIME(6)                                                                                              NULL COMMENT 'Actual administration time, required for ADMINISTERED',
    recorded_at                  DATETIME(6)                                                                                              NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Server recording timestamp',
    device_recorded_at           DATETIME(6)                                                                                              NULL COMMENT 'Device recording timestamp for offline synchronization',
    recorder_id                  BIGINT                                                                                                   NULL COMMENT 'Account ID of recorder or NULL if SYSTEM',
    authority_basis              ENUM ('ASSIGNED', 'SHIFT_LEAD', 'FLOOR_SHARED', 'LEAVE_MEDICATION', 'REPORTED_BY_OTHERS', 'SYSTEM')     NOT NULL COMMENT 'Authority justification',
    reported_by_type             VARCHAR(30)                                                                                              NULL COMMENT 'Reporter type if REPORTED_BY_OTHERS (e.g. RELATIVE, RESIDENT)',
    reported_by_id               BIGINT                                                                                                   NULL COMMENT 'Reporter ID if known',
    reported_by_name             VARCHAR(255)                                                                                             NULL COMMENT 'Reporter full name if external',
    relationship                 VARCHAR(100)                                                                                             NULL COMMENT 'Relationship to resident',
    adverse_reaction             VARCHAR(2000)                                                                                            NULL COMMENT 'Adverse reaction description, triggers alert',
    reason                       VARCHAR(2000)                                                                                            NULL COMMENT 'Reason for refusal or skipping, mandatory for REFUSED/NOT_ADMINISTERED',
    note                         VARCHAR(2000)                                                                                            NULL COMMENT 'General nursing notes',
    late_flag                    BOOLEAN                                                                                                  NOT NULL DEFAULT FALSE COMMENT 'Flag set when administered after window_to',
    offline_flag                 BOOLEAN                                                                                                  NOT NULL DEFAULT FALSE COMMENT 'Flag set when recorded offline',
    synced_after_permission_loss BOOLEAN                                                                                                  NOT NULL DEFAULT FALSE COMMENT 'Flag set when synced after losing permission',
    family_medication_lot_id     BIGINT                                                                                                   NULL COMMENT 'Family sent medication lot ID consumed',
    leave_handover_id            BIGINT                                                                                                   NULL COMMENT 'Leave medication handover record ID',
    CONSTRAINT fk_dose_result_dose FOREIGN KEY (dose_id) REFERENCES medication_dose (id) ON DELETE RESTRICT,
    CONSTRAINT chk_dose_result_administered CHECK (result_type <> 'ADMINISTERED' OR administered_at IS NOT NULL),
    CONSTRAINT chk_dose_result_time CHECK (administered_at IS NULL OR administered_at <= COALESCE(device_recorded_at, recorded_at)),
    CONSTRAINT chk_dose_result_reason CHECK (result_type NOT IN ('REFUSED', 'NOT_ADMINISTERED') OR reason IS NOT NULL),
    KEY idx_dose_result_resident_recorded (resident_id, recorded_at),
    KEY idx_dose_result_recorder_recorded (recorder_id, recorded_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- 3. dose_review - Group 2 (Reviews for special control offline or conflicting duplicate records)
CREATE TABLE dose_review
(
    id                 BIGINT                                                         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    dose_result_id     BIGINT                                                         NULL UNIQUE COMMENT 'Dose result awaiting review',
    prn_usage_id       BIGINT                                                         NULL COMMENT 'PRN usage record ID if PRN review',
    floor_area_id      BIGINT                                                         NULL COMMENT 'Floor area context',
    reviewer_candidate JSON                                                           NULL COMMENT 'Target reviewer account IDs',
    status             ENUM ('PENDING_REVIEW', 'ACCEPTED', 'REQUIRES_CORRECTION')     NOT NULL DEFAULT 'PENDING_REVIEW' COMMENT 'Review status',
    reviewer_id        BIGINT                                                         NULL COMMENT 'Account ID of actual reviewer',
    reviewed_at        DATETIME(6)                                                    NULL COMMENT 'Timestamp of review completion',
    note               VARCHAR(2000)                                                  NULL COMMENT 'Reviewer comments',
    version            INT                                                            NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
    created_at         DATETIME(6)                                                    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at         DATETIME(6)                                                    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_dose_review_result FOREIGN KEY (dose_result_id) REFERENCES dose_result (id) ON DELETE RESTRICT,
    KEY idx_dose_review_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- 4. Triggers

DELIMITER $$

-- Trigger: trg_medication_dose_terminal_lock (FR-022)
-- Prevents updating status when the dose is already in a terminal status or ended_at has been set.
CREATE TRIGGER trg_medication_dose_terminal_lock
BEFORE UPDATE ON medication_dose
FOR EACH ROW
BEGIN
    IF OLD.status IN ('MISSED', 'ADMINISTERED', 'REFUSED', 'NOT_ADMINISTERED', 'CANCELLED') THEN
        IF NEW.status <> OLD.status THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Medication dose is in terminal state and locked against updates (FR-022)';
        END IF;
    END IF;

    IF OLD.ended_at IS NOT NULL AND NEW.status <> OLD.status THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Medication dose suspension has expired and is locked (FR-022)';
    END IF;
END$$

-- Trigger: trg_medication_dose_no_delete
-- Enforces no deletion of medication doses
CREATE TRIGGER trg_medication_dose_no_delete
BEFORE DELETE ON medication_dose
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Medication doses cannot be deleted';
END$$

-- Trigger: trg_dose_result_no_update (Group 3: DBR-13, BR-M07-12)
CREATE TRIGGER trg_dose_result_no_update
BEFORE UPDATE ON dose_result
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Dose result is append-only (Group 3: DBR-13, BR-M07-12)';
END$$

-- Trigger: trg_dose_result_no_delete (Group 3)
CREATE TRIGGER trg_dose_result_no_delete
BEFORE DELETE ON dose_result
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Dose result cannot be deleted (Group 3)';
END$$

DELIMITER ;
