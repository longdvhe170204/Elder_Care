-- 006 T012: Prescription tables for Feature 006 (Module M07) - Medication Management.
-- Tables: prescription (Group 2), prescription_active_ingredient (Group 3), prescription_issue (Group 3), prescription_basis (Group 3).
-- Reference: data-model section 4; research D-05, D-14, D-16; spec FR-004 -> FR-017, BR-M07-05, BR-M07-06, BR-M07-16.

-- 1. prescription - Group 2 (Business with lifecycle)
CREATE TABLE prescription
(
    id                     BIGINT                                                               NOT NULL AUTO_INCREMENT PRIMARY KEY,
    resident_id            BIGINT                                                               NOT NULL COMMENT 'Resident logic ID (001)',
    medication_id          BIGINT                                                               NOT NULL COMMENT 'Medication catalog ID',
    dosage                 DECIMAL(10, 2)                                                       NOT NULL COMMENT 'Dosage amount',
    dosage_unit            VARCHAR(50)                                                          NOT NULL COMMENT 'Dosage unit (pill, ml, etc.)',
    route                  VARCHAR(50)                                                          NOT NULL COMMENT 'Administration route (Oral, Injection, etc.)',
    prescription_type      ENUM ('PERIODIC', 'PRN')                                             NOT NULL COMMENT 'Prescription type: PERIODIC (DINH_KY) or PRN',
    frequency_type         ENUM ('TIMED_SLOTS', 'EVERY_N_HOURS', 'DAYS_OF_WEEK')                NULL COMMENT 'Frequency type for PERIODIC',
    timed_slots            JSON                                                                 NULL COMMENT 'Array of HH:mm strings',
    every_n_hours          SMALLINT                                                             NULL COMMENT 'Every N hours',
    first_slot             DATETIME(6)                                                          NULL COMMENT 'First slot for EVERY_N_HOURS series',
    days_of_week           SET ('1', '2', '3', '4', '5', '6', '7')                              NULL COMMENT 'Days of week (1=Sun..7=Sat)',
    min_interval_minutes   INT                                                                  NULL COMMENT 'Minimum interval in minutes for PRN',
    max_doses_per_24h      SMALLINT                                                             NULL COMMENT 'Maximum PRN doses in rolling 24 hours',
    started_at             DATETIME(6)                                                          NOT NULL COMMENT 'Start effective time',
    ended_at               DATE                                                                 NULL COMMENT 'End date of prescription',
    suspended_from         DATETIME(6)                                                          NULL COMMENT 'Scheduled suspension/discontinuation time',
    medication_source      ENUM ('FACILITY', 'FAMILY_SENT')                                     NOT NULL COMMENT 'Source: VIEN or GIA_DINH_GUI',
    prescriber_type        ENUM ('INTERNAL', 'EXTERNAL')                                        NOT NULL COMMENT 'Prescriber: NOI_BO or BEN_NGOAI',
    facility_name          VARCHAR(255)                                                         NULL COMMENT 'External facility name',
    prescriber_name        VARCHAR(255)                                                         NULL COMMENT 'External prescriber doctor name',
    prescriber_id          BIGINT                                                               NULL COMMENT 'Internal prescriber doctor ID',
    prescribed_at          DATE                                                                 NULL COMMENT 'External prescription date',
    prescription_file_id   BIGINT                                                               NULL COMMENT 'Prescription document/photo file ID',
    instruction_note       VARCHAR(2000)                                                        NULL COMMENT 'Instruction note, the only field editable via If-Match',
    status                 ENUM ('PENDING_EFFECTIVE', 'ACTIVE', 'SUSPENDED', 'DISCONTINUED', 'EXPIRED') NOT NULL DEFAULT 'ACTIVE' COMMENT 'Prescription lifecycle status',
    suspension_reason      ENUM ('ORDER', 'RECONCILIATION_PENDING')                             NULL COMMENT 'Suspension reason: LENH or CHO_DOI_CHIEU',
    pending_admission      BOOLEAN                                                              NOT NULL DEFAULT FALSE COMMENT 'Flag for prescriptions waiting for admission completion',
    replaces_id            BIGINT                                                               NULL UNIQUE COMMENT 'ID of prescription this replaces (DBR-14)',
    termination_reason     ENUM ('DISCONTINUED', 'REPLACED', 'EXPIRED', 'RECONCILIATION', 'FINAL_STATE') NULL COMMENT 'Reason for termination',
    reconciliation_ticket_id BIGINT                                                             NULL COMMENT 'Reconciliation ticket ID that created this prescription',
    expiration_warned_at   DATETIME(6)                                                          NULL COMMENT 'Time when expiration reminder was sent',
    creator_id             BIGINT                                                               NULL COMMENT 'Account ID of creator',
    created_at             DATETIME(6)                                                          NOT NULL COMMENT 'Creation timestamp',
    version                INT                                                                  NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
    CONSTRAINT fk_prescription_medication FOREIGN KEY (medication_id) REFERENCES medication (id) ON DELETE RESTRICT,
    CONSTRAINT fk_prescription_replaces FOREIGN KEY (replaces_id) REFERENCES prescription (id) ON DELETE RESTRICT,
    CONSTRAINT chk_prescription_prescriber CHECK (prescriber_type = 'INTERNAL' OR (facility_name IS NOT NULL AND prescriber_name IS NOT NULL AND prescribed_at IS NOT NULL)),
    CONSTRAINT chk_prescription_type CHECK (
        (prescription_type = 'PERIODIC' AND frequency_type IS NOT NULL AND min_interval_minutes IS NULL AND max_doses_per_24h IS NULL)
        OR (prescription_type = 'PRN' AND frequency_type IS NULL AND min_interval_minutes IS NOT NULL AND max_doses_per_24h IS NOT NULL)
    ),
    CONSTRAINT chk_prescription_prn_interval CHECK (min_interval_minutes IS NULL OR min_interval_minutes > 0),
    CONSTRAINT chk_prescription_prn_max_doses CHECK (max_doses_per_24h IS NULL OR max_doses_per_24h >= 1),
    KEY idx_prescription_resident_status (resident_id, status),
    KEY idx_prescription_status_started (status, started_at),
    KEY idx_prescription_status_suspended (status, suspended_from),
    KEY idx_prescription_status_ended (status, ended_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- 2. prescription_active_ingredient - Group 3 (Snapshot of active ingredients at prescription creation)
CREATE TABLE prescription_active_ingredient
(
    prescription_id BIGINT NOT NULL COMMENT 'Prescription ID',
    allergen_id     BIGINT NOT NULL COMMENT 'Allergen logic ID from 001',
    PRIMARY KEY (prescription_id, allergen_id),
    CONSTRAINT fk_presc_act_ing_presc FOREIGN KEY (prescription_id) REFERENCES prescription (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- 3. prescription_issue - Group 3 (Confirmed duplicate ingredient or allergy conflict issues)
CREATE TABLE prescription_issue
(
    id                        BIGINT                                                              NOT NULL AUTO_INCREMENT PRIMARY KEY,
    prescription_id           BIGINT                                                              NULL COMMENT 'Prescription ID (null when checking on reconciliation ticket)',
    reconciliation_line_id    BIGINT                                                              NULL COMMENT 'Reconciliation line ID',
    line_number               INT                                                                 NULL COMMENT 'Reconciliation line number',
    issue_type                ENUM ('DUPLICATE_ACTIVE_INGREDIENT', 'ALLERGY_CONFLICT', 'OTHER_ALLERGY_REVIEW') NOT NULL COMMENT 'Issue type',
    issue_code                VARCHAR(64)                                                         NULL COMMENT 'Distinct issue code',
    duplicate_prescription_id BIGINT                                                              NULL COMMENT 'Existing prescription with duplicate ingredient',
    allergy_item_id           BIGINT                                                              NULL COMMENT 'Health allergy record ID from 001',
    severity                  VARCHAR(50)                                                         NULL COMMENT 'Allergy severity',
    active_ingredient         VARCHAR(255)                                                        NULL COMMENT 'Ingredient / allergen name',
    manual_review_required    BOOLEAN                                                             NOT NULL DEFAULT FALSE COMMENT 'Unchecked custom allergy requiring doctor review',
    confirmation_reason       VARCHAR(2000)                                                       NULL COMMENT 'Confirmation reason provided by prescriber',
    confirmed_by_id           BIGINT                                                              NULL COMMENT 'Account ID of user confirming issue',
    occurred_at               DATETIME(6)                                                         NOT NULL COMMENT 'Timestamp of issue record',
    KEY idx_issue_prescription (prescription_id),
    KEY idx_issue_recon_line (reconciliation_line_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- 4. prescription_basis - Group 3 (External medical basis for nurse commands on external prescriptions)
CREATE TABLE prescription_basis
(
    id               BIGINT                                                               NOT NULL AUTO_INCREMENT PRIMARY KEY,
    prescription_id  BIGINT                                                               NOT NULL COMMENT 'Prescription ID',
    command          ENUM ('SUSPEND', 'RESUME', 'DISCONTINUE', 'EXTEND', 'CHANGE_DOSE')    NOT NULL COMMENT 'Target command',
    facility_name    VARCHAR(255)                                                         NOT NULL COMMENT 'External medical facility name',
    prescriber_name  VARCHAR(255)                                                         NOT NULL COMMENT 'Prescribing physician name',
    prescribed_date  DATE                                                                 NOT NULL COMMENT 'Date prescribed / indicated',
    basis_type       ENUM ('NEW_PRESCRIPTION', 'DISCHARGE_PAPER', 'PHONE_ORDER')           NOT NULL COMMENT 'Basis type',
    caller_name      VARCHAR(255)                                                         NULL COMMENT 'Caller name when phone order',
    receiver_name    VARCHAR(255)                                                         NULL COMMENT 'Receiver name when phone order',
    called_at        DATETIME(6)                                                          NULL COMMENT 'Phone call timestamp',
    document_file_id BIGINT                                                               NULL COMMENT 'Attached document file ID',
    recorder_id      BIGINT                                                               NOT NULL COMMENT 'Account ID of nurse recording basis',
    recorded_at      DATETIME(6)                                                          NOT NULL COMMENT 'Timestamp of basis record',
    CONSTRAINT fk_presc_basis_presc FOREIGN KEY (prescription_id) REFERENCES prescription (id) ON DELETE RESTRICT,
    CONSTRAINT chk_prescription_basis_type CHECK (
        (basis_type = 'PHONE_ORDER' AND caller_name IS NOT NULL AND receiver_name IS NOT NULL AND called_at IS NOT NULL)
        OR (basis_type <> 'PHONE_ORDER' AND document_file_id IS NOT NULL)
    ),
    KEY idx_basis_prescription (prescription_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Triggers for prescription table (FR-013, FR-014, DBR-14)
DELIMITER $$
CREATE TRIGGER trg_prescription_lock
    BEFORE UPDATE
    ON prescription
    FOR EACH ROW
BEGIN
    -- 1. Block status changes when old status is DISCONTINUED or EXPIRED
    IF OLD.status IN ('DISCONTINUED', 'EXPIRED') AND NEW.status <> OLD.status THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Final status of prescription cannot be modified (FR-013, DBR-14)';
    END IF;

    -- 2. Block modifications to fixed prescription fields once saved
    IF NOT (NEW.medication_id <=> OLD.medication_id)
       OR NOT (NEW.dosage <=> OLD.dosage)
       OR NOT (NEW.dosage_unit <=> OLD.dosage_unit)
       OR NOT (NEW.route <=> OLD.route)
       OR NOT (NEW.prescription_type <=> OLD.prescription_type)
       OR NOT (NEW.frequency_type <=> OLD.frequency_type)
       OR NOT (NEW.timed_slots <=> OLD.timed_slots)
       OR NOT (NEW.every_n_hours <=> OLD.every_n_hours)
       OR NOT (NEW.first_slot <=> OLD.first_slot)
       OR NOT (NEW.days_of_week <=> OLD.days_of_week)
       OR NOT (NEW.min_interval_minutes <=> OLD.min_interval_minutes)
       OR NOT (NEW.max_doses_per_24h <=> OLD.max_doses_per_24h)
       OR NOT (NEW.medication_source <=> OLD.medication_source)
       OR NOT (NEW.prescriber_type <=> OLD.prescriber_type)
       OR NOT (NEW.facility_name <=> OLD.facility_name)
       OR NOT (NEW.prescriber_name <=> OLD.prescriber_name)
       OR NOT (NEW.prescriber_id <=> OLD.prescriber_id)
       OR NOT (NEW.prescribed_at <=> OLD.prescribed_at)
       OR NOT (NEW.prescription_file_id <=> OLD.prescription_file_id)
       OR NOT (NEW.replaces_id <=> OLD.replaces_id)
    THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Fixed fields of saved prescription cannot be modified (FR-014, DBR-14)';
    END IF;

    -- 3. Block started_at modification unless completing admission
    IF NOT (NEW.started_at <=> OLD.started_at) AND NOT (OLD.pending_admission = 1 AND NEW.pending_admission = 0) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Prescription start time cannot be modified unless admission is completed (FR-034a)';
    END IF;
END$$

CREATE TRIGGER trg_prescription_no_delete
    BEFORE DELETE
    ON prescription
    FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'prescription cannot be deleted (FR-014)';
END$$

-- Group 3 append-only triggers
CREATE TRIGGER trg_prescription_active_ingredient_no_update
    BEFORE UPDATE
    ON prescription_active_ingredient
    FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'prescription_active_ingredient is append-only (FR-007)';
END$$

CREATE TRIGGER trg_prescription_active_ingredient_no_delete
    BEFORE DELETE
    ON prescription_active_ingredient
    FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'prescription_active_ingredient is append-only (FR-007)';
END$$

CREATE TRIGGER trg_prescription_issue_no_update
    BEFORE UPDATE
    ON prescription_issue
    FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'prescription_issue is append-only (FR-007)';
END$$

CREATE TRIGGER trg_prescription_issue_no_delete
    BEFORE DELETE
    ON prescription_issue
    FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'prescription_issue is append-only (FR-007)';
END$$

CREATE TRIGGER trg_prescription_basis_no_update
    BEFORE UPDATE
    ON prescription_basis
    FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'prescription_basis is append-only (FR-013, BR-M07-16)';
END$$

CREATE TRIGGER trg_prescription_basis_no_delete
    BEFORE DELETE
    ON prescription_basis
    FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'prescription_basis is append-only (FR-013, BR-M07-16)';
END$$
DELIMITER ;

-- Permissions
GRANT INSERT, UPDATE ON prescription TO 'eldercare_app'@'%';
GRANT INSERT ON prescription_active_ingredient TO 'eldercare_app'@'%';
GRANT INSERT ON prescription_issue TO 'eldercare_app'@'%';
GRANT INSERT ON prescription_basis TO 'eldercare_app'@'%';
