-- 006 T005: Foundation tables of Feature 006 (Module M07) - Medication Management.
-- Includes: resident_medication (per-resident lock, reconciliation deadline), m07_job_run (job execution log),
-- medication (medication catalog - group 1), medication_active_ingredient (active ingredients).
-- Reference: data-model sections 2, 3; research D-01, D-03, D-04, D-19.

-- 1. resident_medication - Technical group 2
-- Resident-level lock target, tracks medication reconciliation deadline upon admission or hospital return.
CREATE TABLE resident_medication
(
    resident_id              BIGINT                                NOT NULL PRIMARY KEY COMMENT 'Primary key, resident logical ID',
    reconciliation_due_at    DATETIME(6)                           NULL COMMENT 'Reconciliation deadline timestamp',
    reconciliation_reason    ENUM ('ADMISSION', 'HOSPITAL_RETURN') NULL COMMENT 'Reconciliation deadline reason (ADMISSION, HOSPITAL_RETURN)',
    reconciliation_warned_at DATETIME(6)                           NULL COMMENT 'Timestamp when reconciliation warning was emitted',
    version                  INT                                   NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
    CONSTRAINT chk_resident_med_reconciliation_reason CHECK (reconciliation_due_at IS NULL OR reconciliation_reason IS NOT NULL)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- 2. m07_job_run - Technical table
-- Tracks job execution runs (research D-19) to ensure idempotent execution per checkpoint, including catch-up runs.
CREATE TABLE m07_job_run
(
    job_key          VARCHAR(64)  NOT NULL COMMENT 'Job key: NCT_CHUA_PHU_TRACH, TANG_KHONG_DIEU_DUONG, HET_HAN_DON, NHAC_HET_HAN_DON, HET_HAN_DUNG',
    checkpoint       VARCHAR(128) NOT NULL COMMENT 'Checkpoint identifier e.g. shiftId:residentId, floor:shiftId or date yyyy-MM-dd',
    result           VARCHAR(500) NULL COMMENT 'Execution result summary',
    scheduler_run_id BIGINT       NULL COMMENT 'Scheduler run reference from module 000',
    completed_at     DATETIME(6)  NULL COMMENT 'Completion timestamp',
    PRIMARY KEY (job_key, checkpoint)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- 3. medication - Catalog group 1 (FR-001, FR-002, AC-01)
CREATE TABLE medication
(
    id                    BIGINT                      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name                  VARCHAR(200)                NOT NULL COMMENT 'Medication name',
    concentration         VARCHAR(100)                NOT NULL COMMENT 'Concentration/strength (e.g. 500mg, 10ml)',
    dosage_form           VARCHAR(100)                NOT NULL COMMENT 'Dosage form (tablet, syrup, solution, etc.)',
    default_route         VARCHAR(100)                NOT NULL COMMENT 'Default administration route (Oral, Injection, Topical, etc.)',
    usage_unit            VARCHAR(50)                 NOT NULL COMMENT 'Usage unit (tablet, ml, sachet, etc.)',
    before_window_minutes SMALLINT                    NULL COMMENT 'Before window (minutes), null uses CFG-M07-01',
    after_window_minutes  SMALLINT                    NULL COMMENT 'After window (minutes), null uses CFG-M07-01',
    special_control       BOOLEAN                     NOT NULL DEFAULT FALSE COMMENT 'Special control medication flag',
    status                ENUM ('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE' COMMENT 'Catalog active status',
    version               INT                         NOT NULL DEFAULT 0 COMMENT 'Catalog version',
    CONSTRAINT uq_medication_identity UNIQUE (name, concentration, dosage_form),
    CONSTRAINT chk_medication_window_before CHECK (before_window_minutes IS NULL OR before_window_minutes >= 0),
    CONSTRAINT chk_medication_window_after CHECK (after_window_minutes IS NULL OR after_window_minutes >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- 4. medication_active_ingredient - Group 1 child table
-- Links medication with allergen catalog from 001 for allergy checking and duplicate active ingredient verification.
CREATE TABLE medication_active_ingredient
(
    medication_id BIGINT NOT NULL COMMENT 'Foreign key to medication',
    allergen_id   BIGINT NOT NULL COMMENT 'Allergen logic reference from 001',
    PRIMARY KEY (medication_id, allergen_id),
    CONSTRAINT fk_med_active_ingredient_med FOREIGN KEY (medication_id) REFERENCES medication (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Account permissions per R-05
GRANT INSERT, UPDATE, DELETE ON medication TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE, DELETE ON medication_active_ingredient TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE ON resident_medication TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE ON m07_job_run TO 'eldercare_app'@'%';
