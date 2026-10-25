-- 004 T015: lượt đăng ký tiếp nhận và ý kiến của người cao tuổi về việc vào ở (data-model mục 3; FR-001, FR-002, FR-004a,
-- BR-M02-15; research T-09a). Cú pháp dùng được ở MySQL 8.0 và 8.4 (research R-01 của 000). Người cao tuổi là định danh
-- logic của 001 nên không có khóa ngoại sang module khác (constitution X).

-- Lượt đăng ký, nhóm 2. Tối đa một lượt mở (Đang xử lý hoặc Đang chờ) cho mỗi người nhờ cột sinh open_resident_id có
-- UNIQUE (FR-001). Lượt Đã tiếp nhận, Đã hủy không sửa được (trigger).
CREATE TABLE admission_application
(
    id                      BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    resident_id             BIGINT      NOT NULL,
    application_date        DATE        NOT NULL COMMENT 'Không đổi khi đã có hồ sơ chờ (BR-M02-15), kiểm ở dịch vụ',
    contact_person          VARCHAR(200) NULL,
    needs                   VARCHAR(2000) NULL,
    note                    VARCHAR(2000) NULL,
    desired_residence_type  ENUM ('LONG_TERM_RESIDENTIAL', 'SHORT_TERM_RESIDENTIAL', 'DAY_CARE') NOT NULL,
    expected_care_level     ENUM ('BASIC', 'REGULAR', 'SPECIAL') NOT NULL,
    desired_admission_date  DATE        NULL,
    status                  ENUM ('IN_PROGRESS', 'WAITING', 'ADMITTED', 'CANCELLED') NOT NULL DEFAULT 'IN_PROGRESS',
    open_resident_id        BIGINT GENERATED ALWAYS AS (IF(status IN ('IN_PROGRESS', 'WAITING'), resident_id, NULL)) STORED
        COMMENT 'UNIQUE: tối đa một lượt mở mỗi người (FR-001)',
    opinion_at_admission_id BIGINT      NULL COMMENT 'Ý kiến Hiệu lực lúc Hoàn tất tiếp nhận; đặt một lần (FR-004a)',
    version                 INT         NOT NULL DEFAULT 0,
    created_by              BIGINT      NOT NULL,
    created_at              DATETIME(6) NOT NULL,
    CONSTRAINT uq_admission_application_open UNIQUE (open_resident_id),
    KEY ix_admission_application_resident (resident_id, id),
    KEY ix_admission_application_status (status, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Ý kiến về việc vào ở, nhóm 2, nội dung bất biến: mỗi lần ghi một dòng; ý kiến mới thay ý kiến Hiệu lực cũ, ý kiến cũ
-- vẫn tra cứu được (FR-001). Cột nguồn theo mẫu cột nguồn của 001 (BR-M01-14).
CREATE TABLE admission_opinion
(
    id                      BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    application_id          BIGINT      NOT NULL,
    opinion                 ENUM ('AGREE', 'DISAGREE', 'UNABLE_TO_EXPRESS') NOT NULL,
    source_type             ENUM ('RESIDENT', 'FAMILY_MEMBER', 'OBSERVING_STAFF') NOT NULL,
    source_family_member_id BIGINT      NULL COMMENT 'Hồ sơ người thân do 012 giữ nên không có khóa ngoại',
    note                    VARCHAR(2000) NULL,
    recorded_by             BIGINT      NOT NULL,
    recorded_at             DATETIME(6) NOT NULL,
    status                  ENUM ('ACTIVE', 'SUPERSEDED') NOT NULL DEFAULT 'ACTIVE',
    superseded_by_id        BIGINT      NULL,
    active_application_id   BIGINT GENERATED ALWAYS AS (IF(status = 'ACTIVE', application_id, NULL)) STORED
        COMMENT 'UNIQUE: tối đa một ý kiến Hiệu lực mỗi lượt (FR-001)',
    CONSTRAINT uq_admission_opinion_active UNIQUE (active_application_id),
    KEY ix_admission_opinion_application (application_id, id),
    CONSTRAINT fk_admission_opinion_application FOREIGN KEY (application_id) REFERENCES admission_application (id) ON DELETE RESTRICT,
    CONSTRAINT fk_admission_opinion_superseded_by FOREIGN KEY (superseded_by_id) REFERENCES admission_opinion (id) ON DELETE RESTRICT,
    CONSTRAINT ck_admission_opinion_source CHECK (
        (source_type = 'FAMILY_MEMBER') = (source_family_member_id IS NOT NULL))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Dùng USER() (tài khoản đang kết nối): trong trigger CURRENT_USER() là tài khoản tạo trigger. Thông báo bằng ASCII.
DELIMITER $$
CREATE TRIGGER trg_admission_application_guard
    BEFORE UPDATE
    ON admission_application
    FOR EACH ROW
BEGIN
    IF OLD.status IN ('ADMITTED', 'CANCELLED') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'a closed admission application cannot change (FR-001)';
    END IF;
    IF NOT (NEW.resident_id <=> OLD.resident_id
        AND NEW.created_by <=> OLD.created_by
        AND NEW.created_at <=> OLD.created_at) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'resident and creation stamp of an admission application are immutable';
    END IF;
    IF OLD.opinion_at_admission_id IS NOT NULL AND NOT (NEW.opinion_at_admission_id <=> OLD.opinion_at_admission_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'opinion_at_admission_id is set once (FR-004a)';
    END IF;
END$$

CREATE TRIGGER trg_admission_application_no_delete
    BEFORE DELETE
    ON admission_application
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'admission_application rows are removed only by the retention job (FR-051)';
    END IF;
END$$

-- Nội dung ý kiến không sửa; chỉ đổi Hiệu lực -> Được thay thế, và tham chiếu tới ý kiến thay thế đặt đúng một lần từ NULL.
CREATE TRIGGER trg_admission_opinion_only_supersede
    BEFORE UPDATE
    ON admission_opinion
    FOR EACH ROW
BEGIN
    IF NOT (NEW.application_id <=> OLD.application_id
        AND NEW.opinion <=> OLD.opinion
        AND NEW.source_type <=> OLD.source_type
        AND NEW.source_family_member_id <=> OLD.source_family_member_id
        AND NEW.note <=> OLD.note
        AND NEW.recorded_by <=> OLD.recorded_by
        AND NEW.recorded_at <=> OLD.recorded_at) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'admission opinion content is immutable (FR-001)';
    END IF;
    IF OLD.status = 'ACTIVE' AND NEW.status <> 'SUPERSEDED' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'an active admission opinion can only be superseded (FR-001)';
    END IF;
    IF OLD.status = 'SUPERSEDED' THEN
        IF NOT (NEW.status <=> OLD.status
            AND (NEW.superseded_by_id <=> OLD.superseded_by_id OR OLD.superseded_by_id IS NULL)) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'a superseded admission opinion cannot change (FR-001)';
        END IF;
    END IF;
END$$

CREATE TRIGGER trg_admission_opinion_no_delete
    BEFORE DELETE
    ON admission_opinion
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'admission_opinion rows are removed only by the retention job (FR-051)';
    END IF;
END$$
DELIMITER ;

GRANT INSERT, UPDATE ON admission_application TO 'eldercare_app'@'%';
GRANT INSERT ON admission_opinion TO 'eldercare_app'@'%';
GRANT UPDATE (status, superseded_by_id) ON admission_opinion TO 'eldercare_app'@'%';
GRANT DELETE ON admission_application TO 'eldercare_retention'@'%';
GRANT DELETE ON admission_opinion TO 'eldercare_retention'@'%';
