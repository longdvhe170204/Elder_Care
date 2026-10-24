-- 001 T037: lần đánh giá và nhu cầu chăm sóc được chọn (data-model mục 7; research H-10; DBR-05, BR-M01-09, BR-M01-13,
-- FR-026, FR-034). Cú pháp dùng được ở MySQL 8.0 và 8.4 (research R-01 của 000). resident_id là khóa ngoại tới `resident`;
-- performed_by, confirmed_by, discarded_by là định danh logic của tài khoản (không có khóa ngoại chéo module,
-- constitution X). Mức chăm sóc hiện hành và lịch sử nằm ở `resident`, `care_level_history` (V001_02).

-- Nhóm 3 có bản nháp: sửa được khi DRAFT; CONFIRMED và DISCARDED không sửa (trigger), chỉ đính chính qua 000
-- (M01.ASSESSMENT). open_draft: tối đa một bản nháp mỗi người (BR-M01-13); confirmed_initial: tối đa một đánh giá đầu vào
-- đã xác nhận (FR-034).
CREATE TABLE assessment
(
    id                     BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    resident_id            BIGINT        NOT NULL,
    type                   ENUM ('INITIAL', 'REASSESSMENT') NOT NULL,
    basis                  VARCHAR(1000) NULL COMMENT 'Lý do, căn cứ của lần đánh giá',
    performed_by           BIGINT        NOT NULL COMMENT 'Người lập',
    performed_at           DATETIME(6)   NOT NULL,
    comment                TEXT          NULL COMMENT 'Nhận xét đánh giá; bắt buộc khi xác nhận (DBR-05)',
    care_note              VARCHAR(1000) NULL COMMENT 'Lưu ý chăm sóc hiện trên thẻ người cao tuổi (BR-M01-10)',
    care_level             VARCHAR(20)   NULL COMMENT 'Bắt buộc khi xác nhận (DBR-05); không có mức đề xuất',
    proposed_residence_type ENUM ('LONG_TERM_RESIDENTIAL', 'SHORT_TERM_RESIDENTIAL', 'DAY_CARE') NULL COMMENT 'Chỉ INITIAL, chỉ để tham khảo (FR-033)',
    status                 ENUM ('DRAFT', 'CONFIRMED', 'DISCARDED') NOT NULL DEFAULT 'DRAFT',
    confirmed_by           BIGINT        NULL,
    confirmed_at           DATETIME(6)   NULL,
    discarded_by           BIGINT        NULL,
    discarded_at           DATETIME(6)   NULL,
    discard_reason         VARCHAR(2000) NULL,
    open_draft             BIGINT GENERATED ALWAYS AS (IF(status = 'DRAFT', resident_id, NULL)) STORED,
    confirmed_initial      BIGINT GENERATED ALWAYS AS (IF(type = 'INITIAL' AND status = 'CONFIRMED', resident_id, NULL)) STORED,
    version                INT           NOT NULL DEFAULT 0 COMMENT 'Khóa lạc quan khi sửa nháp',
    CONSTRAINT uq_assessment_open_draft UNIQUE (open_draft),
    CONSTRAINT uq_assessment_confirmed_initial UNIQUE (confirmed_initial),
    CONSTRAINT fk_assessment_resident FOREIGN KEY (resident_id) REFERENCES resident (id) ON DELETE RESTRICT,
    CONSTRAINT fk_assessment_care_level FOREIGN KEY (care_level) REFERENCES care_level (code) ON DELETE RESTRICT,
    CONSTRAINT chk_assessment_confirm CHECK (status <> 'CONFIRMED' OR (comment IS NOT NULL
        AND CHAR_LENGTH(TRIM(comment)) > 0 AND care_level IS NOT NULL
        AND confirmed_by IS NOT NULL AND confirmed_at IS NOT NULL)),
    CONSTRAINT chk_assessment_discard CHECK (status <> 'DISCARDED' OR (discarded_by IS NOT NULL
        AND discarded_at IS NOT NULL AND discard_reason IS NOT NULL AND CHAR_LENGTH(TRIM(discard_reason)) > 0)),
    CONSTRAINT chk_assessment_proposed_residence CHECK (type = 'INITIAL' OR proposed_residence_type IS NULL),
    KEY idx_assessment_resident (resident_id, status, confirmed_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Nhu cầu chăm sóc được chọn trong lần đánh giá (Q-291): không, một hoặc nhiều; không đòi lý do (FR-029).
CREATE TABLE assessment_care_need
(
    assessment_id BIGINT NOT NULL,
    care_need_id  BIGINT NOT NULL,
    PRIMARY KEY (assessment_id, care_need_id),
    CONSTRAINT fk_assessment_care_need_assessment FOREIGN KEY (assessment_id) REFERENCES assessment (id) ON DELETE RESTRICT,
    CONSTRAINT fk_assessment_care_need_need FOREIGN KEY (care_need_id) REFERENCES care_need (id) ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Thông báo bằng ASCII (như V001_02). Dùng USER(): trong trigger CURRENT_USER() là tài khoản tạo trigger.
DELIMITER $$
CREATE TRIGGER trg_assessment_no_update
    BEFORE UPDATE
    ON assessment
    FOR EACH ROW
BEGIN
    IF OLD.status IN ('CONFIRMED', 'DISCARDED') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'a confirmed or discarded assessment is not edited, only corrected (FR-032)';
    END IF;
END$$

CREATE TRIGGER trg_assessment_care_need_no_insert
    BEFORE INSERT
    ON assessment_care_need
    FOR EACH ROW
BEGIN
    IF (SELECT status FROM assessment WHERE id = NEW.assessment_id) <> 'DRAFT' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'care needs of an assessment are edited only while it is a draft (FR-032)';
    END IF;
END$$

-- Job loại bỏ dữ liệu hết hạn (eldercare_retention) xóa được cả khi lần đánh giá cha đã xác nhận (FR-051).
CREATE TRIGGER trg_assessment_care_need_no_delete
    BEFORE DELETE
    ON assessment_care_need
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention'
        AND (SELECT status FROM assessment WHERE id = OLD.assessment_id) <> 'DRAFT' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'care needs of an assessment are edited only while it is a draft (FR-032)';
    END IF;
END$$

-- Chỉ job loại bỏ dữ liệu hết hạn (eldercare_retention) xóa được (FR-051).
CREATE TRIGGER trg_assessment_no_delete
    BEFORE DELETE
    ON assessment
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'assessment rows are removed only by the retention job (FR-051)';
    END IF;
END$$
DELIMITER ;

GRANT INSERT, UPDATE ON assessment TO 'eldercare_app'@'%';
GRANT INSERT, DELETE ON assessment_care_need TO 'eldercare_app'@'%';
GRANT DELETE ON assessment TO 'eldercare_retention'@'%';
GRANT DELETE ON assessment_care_need TO 'eldercare_retention'@'%';
