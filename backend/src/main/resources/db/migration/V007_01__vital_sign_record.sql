-- 007 T012: bản ghi M06 có nháp, gộp thành phần vào JSON (R-15, data-model mục 0.1, 5).
-- Chỉ kéo phần lược đồ phục vụ nguồn đọc B/008 lên trước; không tạo lệnh ghi, job hoặc dữ liệu lâm sàng mẫu.
CREATE TABLE vital_sign_record
(
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    resident_id BIGINT NOT NULL,
    indicator_id BIGINT NOT NULL,
    measured_at DATETIME(6) NOT NULL,
    measured_by BIGINT NOT NULL,
    task_id BIGINT NULL,
    device_measured_at DATETIME(6) NULL,
    offline BOOLEAN NOT NULL DEFAULT FALSE,
    threshold_version_id BIGINT NULL,
    level ENUM ('NORMAL','WARNING','DANGEROUS','NO_THRESHOLD') NOT NULL,
    confirmation_status ENUM ('AWAITING_REMEASUREMENT','CONFIRMED','REMEASURED') NOT NULL,
    remeasurement_deadline DATETIME(6) NULL,
    remeasurement_for BIGINT NULL,
    components JSON NOT NULL,
    labels SET ('IMMEDIATE_ACTION','NOT_REMEASURED','LATE_RECORDED','OFFLINE_RECORDED') NOT NULL,
    note TEXT NULL,
    confirmed_at DATETIME(6) NULL,
    locked_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    created_by BIGINT NOT NULL,
    CONSTRAINT fk_vital_sign_indicator FOREIGN KEY (indicator_id) REFERENCES catalog_item(id) ON DELETE RESTRICT,
    CONSTRAINT fk_vital_sign_remeasurement FOREIGN KEY (remeasurement_for) REFERENCES vital_sign_record(id) ON DELETE RESTRICT,
    CONSTRAINT uq_vital_sign_remeasurement UNIQUE (remeasurement_for),
    CONSTRAINT chk_vital_sign_measured_at CHECK (measured_at <= created_at),
    CONSTRAINT chk_vital_sign_lock CHECK (
        (confirmation_status='AWAITING_REMEASUREMENT' AND confirmed_at IS NULL AND locked_at IS NULL)
        OR (confirmation_status IN ('CONFIRMED','REMEASURED') AND confirmed_at IS NOT NULL
            AND locked_at IS NOT NULL AND locked_at=confirmed_at)),
    CONSTRAINT chk_vital_sign_components CHECK (JSON_SCHEMA_VALID(
        '{"type":"object","minProperties":1,"additionalProperties":{"type":"object","required":["value","level"],"additionalProperties":false,"properties":{"value":{"type":"number","minimum":-99999.99,"maximum":99999.99,"multipleOf":0.01},"level":{"enum":["NORMAL","WARNING","DANGEROUS","NO_THRESHOLD"]}}}}', components)),
    INDEX ix_vital_sign_resident_indicator_time (resident_id,indicator_id,measured_at),
    INDEX ix_vital_sign_confirmation_deadline (confirmation_status,remeasurement_deadline),
    INDEX ix_vital_sign_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='DataGroup=MULTIPLE';

DELIMITER $$
CREATE TRIGGER trg_vital_sign_record_row_lock BEFORE UPDATE ON vital_sign_record FOR EACH ROW
BEGIN
    IF OLD.locked_at IS NOT NULL
       OR OLD.confirmation_status <> 'AWAITING_REMEASUREMENT'
       OR NEW.confirmation_status NOT IN ('CONFIRMED','REMEASURED')
       OR NEW.confirmed_at IS NULL OR NEW.locked_at IS NULL
       OR NOT (NEW.locked_at <=> NEW.confirmed_at)
       OR NOT (NEW.id <=> OLD.id)
       OR NOT (NEW.resident_id <=> OLD.resident_id)
       OR NOT (NEW.indicator_id <=> OLD.indicator_id)
       OR NOT (NEW.measured_at <=> OLD.measured_at)
       OR NOT (NEW.measured_by <=> OLD.measured_by)
       OR NOT (NEW.task_id <=> OLD.task_id)
       OR NOT (NEW.device_measured_at <=> OLD.device_measured_at)
       OR NOT (NEW.offline <=> OLD.offline)
       OR NOT (NEW.threshold_version_id <=> OLD.threshold_version_id)
       OR NOT (NEW.level <=> OLD.level)
       OR NOT (NEW.remeasurement_deadline <=> OLD.remeasurement_deadline)
       OR NOT (NEW.remeasurement_for <=> OLD.remeasurement_for)
       OR NOT (NEW.components <=> OLD.components)
       OR NOT (NEW.note <=> OLD.note)
       OR NOT (NEW.created_at <=> OLD.created_at)
       OR NOT (NEW.created_by <=> OLD.created_by)
       -- SET có mặt nạ bit: chỉ được thêm NOT_REMEASURED (bit 2), không bỏ nhãn cũ.
       OR ((NEW.labels+0) & (OLD.labels+0)) <> (OLD.labels+0)
       OR (((NEW.labels+0) ^ (OLD.labels+0)) & ~2) <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='vital_sign_record is immutable except one confirmation';
    END IF;
END$$
CREATE TRIGGER trg_vital_sign_record_no_delete BEFORE DELETE ON vital_sign_record FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(),'@',1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='vital_sign_record is removed only by retention';
    END IF;
END$$
DELIMITER ;

GRANT INSERT ON vital_sign_record TO 'eldercare_app'@'%';
GRANT UPDATE (confirmation_status,confirmed_at,locked_at,labels) ON vital_sign_record TO 'eldercare_app'@'%';
GRANT DELETE ON vital_sign_record TO 'eldercare_retention'@'%';
