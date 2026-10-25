-- 006 T035: Medication reconciliation tables for Feature 006 (Module M07) - Medication Management.
-- Tables: medication_reconciliation (Group 2/3), reconciliation_item (child), reconciliation_correction_request (Group 2).
-- Reference: data-model section 7; research D-13, D-16; spec FR-034 -> FR-039, FR-034b, BR-M07-09, BR-M07-17, BR-M07-22, BR-M07-25, Q-340.

-- 1. medication_reconciliation (phieu_doi_chieu)
CREATE TABLE medication_reconciliation
(
    id                    BIGINT                                                                    NOT NULL AUTO_INCREMENT PRIMARY KEY,
    resident_id           BIGINT                                                                    NOT NULL COMMENT 'Resident logic ID',
    reason                ENUM ('ADMISSION', 'HOSPITAL_RETURN', 'PROFESSIONAL_REQUEST')             NOT NULL COMMENT 'Reason for reconciliation',
    reason_detail         VARCHAR(2000)                                                             NULL COMMENT 'Detail justification required for PROFESSIONAL_REQUEST',
    status                ENUM ('OPEN', 'PENDING_CONFIRMATION', 'CONFIRMED', 'CANCELLED')           NOT NULL DEFAULT 'OPEN' COMMENT 'Reconciliation workflow status',
    deadline_at           DATETIME(6)                                                               NULL COMMENT 'Deadline for completion (CFG-M07-05); null for pre-admission or professional request',
    overdue_at            DATETIME(6)                                                               NULL COMMENT 'Timestamp when deadline was breached',
    created_by_id         BIGINT                                                                    NOT NULL COMMENT 'Account ID of creator',
    created_at            DATETIME(6)                                                               NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    submitted_by_id       BIGINT                                                                    NULL COMMENT 'Account ID of submitter',
    submitted_at          DATETIME(6)                                                               NULL COMMENT 'Submission timestamp',
    confirmed_by_id       BIGINT                                                                    NULL COMMENT 'Account ID of confirmer (two-person rule)',
    confirmed_at          DATETIME(6)                                                               NULL COMMENT 'Confirmation timestamp',
    return_reason         VARCHAR(2000)                                                             NULL COMMENT 'Reason if returned for edits',
    cancel_reason         VARCHAR(2000)                                                             NULL COMMENT 'Reason if cancelled',
    open_resident_id      BIGINT GENERATED ALWAYS AS (IF(status IN ('OPEN', 'PENDING_CONFIRMATION'), resident_id, NULL)) STORED,
    admission_resident_id BIGINT GENERATED ALWAYS AS (IF(reason = 'ADMISSION' AND status <> 'CANCELLED', resident_id, NULL)) STORED,
    version               INT                                                                       NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',

    CONSTRAINT uq_recon_open_resident UNIQUE (open_resident_id),
    CONSTRAINT uq_recon_admission_resident UNIQUE (admission_resident_id),
    CONSTRAINT chk_recon_two_person CHECK (confirmed_by_id IS NULL OR confirmed_by_id <> submitted_by_id),
    CONSTRAINT chk_recon_prof_reason CHECK (reason <> 'PROFESSIONAL_REQUEST' OR reason_detail IS NOT NULL),
    KEY idx_recon_resident_status (resident_id, status),
    KEY idx_recon_status_deadline (status, deadline_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- 2. reconciliation_item (dong_doi_chieu)
CREATE TABLE reconciliation_item
(
    id                           BIGINT                                                                                  NOT NULL AUTO_INCREMENT PRIMARY KEY,
    reconciliation_id            BIGINT                                                                                  NOT NULL COMMENT 'Parent reconciliation ticket ID',
    item_order                   INT                                                                                     NOT NULL COMMENT 'Line sequence number',
    source                       ENUM ('CURRENT_PRESCRIPTION', 'DISCHARGE_PRESCRIPTION', 'ADMISSION_CURRENT_MEDICATION') NOT NULL COMMENT 'Origin source of medication line',
    prescription_id              BIGINT                                                                                  NULL COMMENT 'Existing prescription ID if source is CURRENT_PRESCRIPTION',
    admission_medication_item_id BIGINT                                                                                  NULL COMMENT 'Item ID from initial admission profile',
    medication_description       VARCHAR(1000)                                                                           NULL COMMENT 'Free-form description for discharge / admission medications',
    decision                     ENUM ('CONTINUE', 'DISCONTINUE', 'CHANGE_DOSE', 'ADD_NEW')                              NOT NULL COMMENT 'Clinical decision on this line',
    reason                       VARCHAR(2000)                                                                           NULL COMMENT 'Reason or clinical justification for decision',
    new_prescription_json        JSON                                                                                    NULL COMMENT 'New prescription parameters for CHANGE_DOSE or ADD_NEW',
    created_by_id                BIGINT                                                                                  NOT NULL COMMENT 'Account ID of line author',
    created_at                   DATETIME(6)                                                                             NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    discontinued_outside_ticket   BOOLEAN                                                                                 NOT NULL DEFAULT FALSE COMMENT 'Flag if existing prescription was stopped outside this ticket',
    created_prescription_id      BIGINT                                                                                  NULL COMMENT 'ID of prescription materialized upon ticket confirmation',

    CONSTRAINT fk_recon_item_ticket FOREIGN KEY (reconciliation_id) REFERENCES medication_reconciliation (id) ON DELETE RESTRICT,
    CONSTRAINT uq_recon_item_prescription UNIQUE (reconciliation_id, prescription_id),
    CONSTRAINT uq_recon_item_admission_med UNIQUE (reconciliation_id, admission_medication_item_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- 3. reconciliation_correction_request (de_nghi_dinh_chinh_phieu)
CREATE TABLE reconciliation_correction_request
(
    id                     BIGINT                                                   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    reconciliation_id      BIGINT                                                   NOT NULL COMMENT 'Reconciliation ticket ID',
    corrected_content_json JSON                                                     NOT NULL COMMENT 'JSON payload of requested corrections',
    reason                 VARCHAR(2000)                                            NOT NULL COMMENT 'Justification for correction',
    created_by_id          BIGINT                                                   NOT NULL COMMENT 'Proposer (must be submitter or confirmer)',
    created_at             DATETIME(6)                                              NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    status                 ENUM ('PENDING_CONFIRMATION', 'CONFIRMED', 'REJECTED')   NOT NULL DEFAULT 'PENDING_CONFIRMATION',
    confirmed_by_id        BIGINT                                                   NULL COMMENT 'Confirmer account ID',
    confirmed_at           DATETIME(6)                                              NULL,
    correction_id          BIGINT                                                   NULL COMMENT 'Reference to common correction record (Module M15)',

    CONSTRAINT fk_recon_corr_ticket FOREIGN KEY (reconciliation_id) REFERENCES medication_reconciliation (id) ON DELETE RESTRICT,
    CONSTRAINT chk_recon_corr_diff_user CHECK (confirmed_by_id IS NULL OR confirmed_by_id <> created_by_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- 4. Triggers
DELIMITER $$

-- Trigger: trg_reconciliation_terminal_lock (FR-039)
CREATE TRIGGER trg_reconciliation_terminal_lock
BEFORE UPDATE ON medication_reconciliation
FOR EACH ROW
BEGIN
    IF OLD.status IN ('CONFIRMED', 'CANCELLED') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cannot update final status reconciliation ticket (FR-039)';
    END IF;
END$$

-- Trigger: trg_reconciliation_no_delete
CREATE TRIGGER trg_reconciliation_no_delete
BEFORE DELETE ON medication_reconciliation
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Reconciliation tickets cannot be deleted';
END$$

DELIMITER ;
