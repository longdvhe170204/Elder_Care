-- 001 T027: hồ sơ sức khỏe ban đầu và mục sức khỏe (data-model mục 5; research H-07, H-08; DBR-04, BR-M01-07, BR-M01-11,
-- BR-M01-12). Cú pháp dùng được ở MySQL 8.0 và 8.4 (research R-01 của 000). resident_id, recorded_by, performed_by là khóa
-- ngoại tới `resident` hoặc định danh logic của tài khoản (không có khóa ngoại chéo module, constitution X).

-- Hồ sơ sức khỏe ban đầu, nhóm 3 có bản nháp: sửa được tới khi xác nhận, sau đó trigger chặn mọi UPDATE; đính chính đi
-- qua 000 (M01.INITIAL_HEALTH_RECORD). Mỗi người cao tuổi tối đa một dòng.
CREATE TABLE initial_health_record
(
    id                  BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    resident_id         BIGINT      NOT NULL,
    blood_type          VARCHAR(10) NULL,
    mobility            TEXT        NULL,
    self_care_ability   TEXT        NULL,
    cognition           TEXT        NULL,
    initial_needs_note  TEXT        NULL,
    nutrition_needs     TEXT        NULL,
    current_medications TEXT        NULL,
    rehabilitation_needs TEXT       NULL,
    performed_by        BIGINT      NOT NULL COMMENT 'Người ghi bản nháp',
    performed_at        DATETIME(6) NOT NULL,
    confirmed_by        BIGINT      NULL COMMENT 'NULL khi còn nháp',
    confirmed_at        DATETIME(6) NULL,
    version             INT         NOT NULL DEFAULT 0 COMMENT 'Khóa lạc quan khi sửa nháp',
    CONSTRAINT uq_initial_health_record_resident UNIQUE (resident_id),
    CONSTRAINT fk_initial_health_record_resident FOREIGN KEY (resident_id) REFERENCES resident (id) ON DELETE RESTRICT,
    CONSTRAINT chk_initial_health_record_confirm CHECK ((confirmed_by IS NULL) = (confirmed_at IS NULL))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Mục sức khỏe, nhóm 2 nhưng nội dung bất biến (DBR-04): dị ứng, bệnh nền, tiền sử. Chỉ loại trừ có lý do (một chiều) và
-- hai cờ kỹ thuật chờ kiểm tra (BR-M01-12) đổi được. Dị ứng loại "khác" có allergen_id rỗng: hệ thống không kiểm tra tự
-- động (FR-018a).
CREATE TABLE health_item
(
    id                       BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    resident_id              BIGINT        NOT NULL,
    type                     ENUM ('ALLERGY', 'CHRONIC_CONDITION', 'MEDICAL_HISTORY') NOT NULL,
    allergen_id              BIGINT        NULL COMMENT 'Chỉ với ALLERGY chọn từ danh mục',
    free_text                VARCHAR(1000) NULL,
    not_auto_checked         BOOLEAN GENERATED ALWAYS AS (type = 'ALLERGY' AND allergen_id IS NULL) STORED,
    severity_id              BIGINT        NULL COMMENT 'Bắt buộc với ALLERGY (BR-M01-11)',
    source_id                BIGINT        NOT NULL,
    recorded_by              BIGINT        NOT NULL,
    recorded_at              DATETIME(6)   NOT NULL,
    status                   ENUM ('ACTIVE', 'EXCLUDED') NOT NULL DEFAULT 'ACTIVE',
    excluded_at              DATETIME(6)   NULL,
    excluded_by              BIGINT        NULL,
    exclusion_reason         VARCHAR(2000) NULL,
    pending_medication_check BOOLEAN       NOT NULL DEFAULT FALSE COMMENT 'Chờ 006 kiểm tra đơn thuốc (BR-M01-12)',
    pending_nutrition_check  BOOLEAN       NOT NULL DEFAULT FALSE COMMENT 'Chờ 011 kiểm tra chế độ ăn (BR-M01-12)',
    version                  INT           NOT NULL DEFAULT 0,
    CONSTRAINT fk_health_item_resident FOREIGN KEY (resident_id) REFERENCES resident (id) ON DELETE RESTRICT,
    CONSTRAINT fk_health_item_allergen FOREIGN KEY (allergen_id) REFERENCES allergen (id) ON DELETE RESTRICT,
    CONSTRAINT fk_health_item_severity FOREIGN KEY (severity_id) REFERENCES allergy_severity (id) ON DELETE RESTRICT,
    CONSTRAINT fk_health_item_source FOREIGN KEY (source_id) REFERENCES information_source (id) ON DELETE RESTRICT,
    CONSTRAINT chk_health_item_content CHECK (allergen_id IS NOT NULL OR free_text IS NOT NULL),
    CONSTRAINT chk_health_item_allergen_type CHECK (allergen_id IS NULL OR type = 'ALLERGY'),
    CONSTRAINT chk_health_item_severity CHECK (type <> 'ALLERGY' OR severity_id IS NOT NULL),
    CONSTRAINT chk_health_item_exclusion CHECK (
        (status = 'ACTIVE' AND excluded_at IS NULL AND excluded_by IS NULL AND exclusion_reason IS NULL)
            OR (status = 'EXCLUDED' AND excluded_at IS NOT NULL AND excluded_by IS NOT NULL
            AND exclusion_reason IS NOT NULL)),
    KEY idx_health_item_resident (resident_id, type, status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Thông báo bằng ASCII (như V001_02). Dùng USER(): trong trigger CURRENT_USER() là tài khoản tạo trigger.
DELIMITER $$
CREATE TRIGGER trg_initial_health_record_no_update
    BEFORE UPDATE
    ON initial_health_record
    FOR EACH ROW
BEGIN
    IF OLD.confirmed_at IS NOT NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'a confirmed initial_health_record is not edited, only corrected (FR-017)';
    END IF;
END$$

CREATE TRIGGER trg_health_item_immutable
    BEFORE UPDATE
    ON health_item
    FOR EACH ROW
BEGIN
    IF NOT (OLD.resident_id <=> NEW.resident_id AND OLD.type <=> NEW.type AND OLD.allergen_id <=> NEW.allergen_id
        AND OLD.free_text <=> NEW.free_text AND OLD.severity_id <=> NEW.severity_id
        AND OLD.source_id <=> NEW.source_id AND OLD.recorded_by <=> NEW.recorded_by
        AND OLD.recorded_at <=> NEW.recorded_at) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'health_item content is immutable (DBR-04)';
    END IF;
    IF OLD.status = 'EXCLUDED' AND NOT (NEW.status <=> 'EXCLUDED' AND OLD.excluded_at <=> NEW.excluded_at
        AND OLD.excluded_by <=> NEW.excluded_by AND OLD.exclusion_reason <=> NEW.exclusion_reason) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'an excluded health_item cannot be reinstated (DBR-04)';
    END IF;
END$$

-- Chỉ job loại bỏ dữ liệu hết hạn (eldercare_retention) xóa được (FR-051).
CREATE TRIGGER trg_initial_health_record_no_delete
    BEFORE DELETE
    ON initial_health_record
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'initial_health_record rows are removed only by the retention job (FR-051)';
    END IF;
END$$

CREATE TRIGGER trg_health_item_no_delete
    BEFORE DELETE
    ON health_item
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'health_item rows are removed only by the retention job (FR-051)';
    END IF;
END$$
DELIMITER ;

GRANT INSERT, UPDATE ON initial_health_record TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE ON health_item TO 'eldercare_app'@'%';
GRANT DELETE ON initial_health_record TO 'eldercare_retention'@'%';
GRANT DELETE ON health_item TO 'eldercare_retention'@'%';
