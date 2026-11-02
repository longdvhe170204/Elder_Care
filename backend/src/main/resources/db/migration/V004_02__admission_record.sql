-- 004 T015, T023, T110: hồ sơ tiếp nhận (data-model mục 0, 3, 4; FR-001, FR-002, FR-004a, FR-007 → FR-018, Q-394; research T-09a).
-- Bảng gộp lượt đăng ký, hồ sơ chờ, ý kiến vào ở theo cột kind (R-15). Người cao tuổi, giường là định danh logic nên không có
-- khóa ngoại sang module khác (constitution X). Cú pháp dùng được ở MySQL 8.0 và 8.4 (research R-01 của 000).
CREATE TABLE admission_record
(
    id                      BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    kind                    ENUM ('APPLICATION', 'WAITING_ENTRY', 'OPINION') NOT NULL,
    resident_id             BIGINT        NOT NULL,
    application_id          BIGINT        NULL COMMENT 'Lượt đăng ký cha của hồ sơ chờ, ý kiến',
    -- Lượt đăng ký
    application_date        DATE          NULL COMMENT 'Không đổi khi đã có hồ sơ chờ (BR-M02-15), kiểm ở dịch vụ',
    contact_person          VARCHAR(200)  NULL,
    needs                   VARCHAR(2000) NULL,
    note                    VARCHAR(2000) NULL COMMENT 'Ghi chú của lượt đăng ký, ý kiến',
    desired_residence_type  ENUM ('LONG_TERM_RESIDENTIAL', 'SHORT_TERM_RESIDENTIAL', 'DAY_CARE') NULL,
    expected_care_level     ENUM ('BASIC', 'REGULAR', 'SPECIAL') NULL,
    desired_admission_date  DATE          NULL,
    opinion_at_admission_id BIGINT        NULL COMMENT 'Ý kiến Hiệu lực lúc Hoàn tất tiếp nhận; đặt một lần (FR-004a)',
    -- Hồ sơ chờ
    care_level              ENUM ('BASIC', 'REGULAR', 'SPECIAL') NULL COMMENT 'Theo FR-008',
    care_level_not_assessed BOOLEAN       NULL COMMENT 'Mức lấy từ lượt đăng ký vì chưa có đánh giá được chấp nhận',
    priority                BOOLEAN       NOT NULL DEFAULT FALSE COMMENT 'Dấu Ưu tiên (Q-394)',
    priority_reason         VARCHAR(2000) NULL,
    held_bed_id             BIGINT        NULL COMMENT 'Giường đang giữ chỗ; hạn giữ là dữ liệu của 003 (FR-014)',
    last_confirmed_at       DATETIME(6)   NULL COMMENT 'Mốc tính CFG-M02-03 (FR-017)',
    -- Ý kiến vào ở
    opinion                 ENUM ('AGREE', 'DISAGREE', 'UNABLE_TO_EXPRESS') NULL,
    source_type             ENUM ('RESIDENT', 'FAMILY_MEMBER', 'OBSERVING_STAFF') NULL,
    source_family_member_id BIGINT        NULL COMMENT 'Hồ sơ người thân do 012 giữ nên không có khóa ngoại',
    superseded_by_id        BIGINT        NULL,
    -- Chung
    status                  ENUM ('IN_PROGRESS', 'WAITING', 'ADMITTED', 'CANCELLED', 'CONTACTED', 'DECLINED',
        'WAITING_CANCELLED', 'ACTIVE', 'SUPERSEDED') NOT NULL,
    version                 INT           NOT NULL DEFAULT 0,
    created_by              BIGINT        NOT NULL,
    created_at              DATETIME(6)   NOT NULL,
    locked_at               DATETIME(6)   NULL COMMENT 'Đặt khi dòng tới trạng thái cuối (constitution XII)',
    open_application_resident BIGINT GENERATED ALWAYS AS (
        IF(kind = 'APPLICATION' AND status IN ('IN_PROGRESS', 'WAITING'), resident_id, NULL)) STORED
        COMMENT 'UNIQUE: tối đa một lượt mở mỗi người (FR-001)',
    open_entry_resident     BIGINT GENERATED ALWAYS AS (
        IF(kind = 'WAITING_ENTRY' AND status IN ('WAITING', 'CONTACTED'), resident_id, NULL)) STORED
        COMMENT 'UNIQUE: tối đa một hồ sơ chờ chưa ở trạng thái cuối mỗi người (FR-007)',
    held_bed_unique         BIGINT GENERATED ALWAYS AS (
        IF(kind = 'WAITING_ENTRY' AND status = 'CONTACTED', held_bed_id, NULL)) STORED
        COMMENT 'UNIQUE: một giường chỉ do một hồ sơ giữ (FR-014)',
    active_opinion_application BIGINT GENERATED ALWAYS AS (
        IF(kind = 'OPINION' AND status = 'ACTIVE', application_id, NULL)) STORED
        COMMENT 'UNIQUE: tối đa một ý kiến Hiệu lực mỗi lượt (FR-001)',
    CONSTRAINT uq_admission_record_open_application UNIQUE (open_application_resident),
    CONSTRAINT uq_admission_record_open_entry UNIQUE (open_entry_resident),
    CONSTRAINT uq_admission_record_held_bed UNIQUE (held_bed_unique),
    CONSTRAINT uq_admission_record_active_opinion UNIQUE (active_opinion_application),
    CONSTRAINT fk_admission_record_application FOREIGN KEY (application_id) REFERENCES admission_record (id) ON DELETE RESTRICT,
    CONSTRAINT fk_admission_record_superseded_by FOREIGN KEY (superseded_by_id) REFERENCES admission_record (id) ON DELETE RESTRICT,
    CONSTRAINT ck_admission_record_status CHECK (
        (kind = 'APPLICATION' AND status IN ('IN_PROGRESS', 'WAITING', 'ADMITTED', 'CANCELLED'))
            OR (kind = 'WAITING_ENTRY' AND status IN ('WAITING', 'CONTACTED', 'ADMITTED', 'DECLINED', 'WAITING_CANCELLED'))
            OR (kind = 'OPINION' AND status IN ('ACTIVE', 'SUPERSEDED'))),
    CONSTRAINT ck_admission_record_application CHECK (kind <> 'APPLICATION' OR (application_date IS NOT NULL
        AND desired_residence_type IS NOT NULL AND expected_care_level IS NOT NULL AND application_id IS NULL)),
    CONSTRAINT ck_admission_record_entry CHECK (kind <> 'WAITING_ENTRY' OR (application_id IS NOT NULL
        AND care_level IS NOT NULL AND care_level_not_assessed IS NOT NULL AND last_confirmed_at IS NOT NULL)),
    CONSTRAINT ck_admission_record_priority CHECK (NOT priority OR priority_reason IS NOT NULL),
    CONSTRAINT ck_admission_record_contacted_has_bed CHECK (status <> 'CONTACTED' OR held_bed_id IS NOT NULL),
    CONSTRAINT ck_admission_record_opinion CHECK (kind <> 'OPINION' OR (application_id IS NOT NULL AND opinion IS NOT NULL
        AND source_type IS NOT NULL AND (source_type = 'FAMILY_MEMBER') = (source_family_member_id IS NOT NULL))),
    KEY ix_admission_record_resident (kind, resident_id, id),
    KEY ix_admission_record_status (kind, status, id),
    KEY ix_admission_record_application (application_id, kind, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
    COMMENT = 'DataGroup=MULTIPLE';

-- Dùng USER() (tài khoản đang kết nối): trong trigger CURRENT_USER() là tài khoản tạo trigger. Thông báo bằng ASCII.
DELIMITER $$
-- Dòng đã khóa không đổi (constitution XII); người, cha, dấu thời gian tạo bất biến; nội dung ý kiến bất biến, ý kiến chỉ đổi
-- Hiệu lực -> Được thay thế; opinion_at_admission_id đặt một lần (FR-001, FR-004a).
CREATE TRIGGER trg_admission_record_row_lock
    BEFORE UPDATE
    ON admission_record
    FOR EACH ROW
BEGIN
    IF OLD.locked_at IS NOT NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'admission_record row is locked (constitution XII)';
    END IF;
    IF NOT (NEW.kind <=> OLD.kind AND NEW.resident_id <=> OLD.resident_id AND NEW.application_id <=> OLD.application_id
        AND NEW.created_by <=> OLD.created_by AND NEW.created_at <=> OLD.created_at) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'resident, parent and creation stamp of admission_record are immutable';
    END IF;
    IF OLD.opinion_at_admission_id IS NOT NULL AND NOT (NEW.opinion_at_admission_id <=> OLD.opinion_at_admission_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'opinion_at_admission_id is set once (FR-004a)';
    END IF;
    IF OLD.kind = 'OPINION' AND NOT (NEW.opinion <=> OLD.opinion AND NEW.source_type <=> OLD.source_type
        AND NEW.source_family_member_id <=> OLD.source_family_member_id AND NEW.note <=> OLD.note) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'admission opinion content is immutable (FR-001)';
    END IF;
    IF OLD.kind = 'OPINION' AND OLD.status = 'SUPERSEDED' AND NEW.status <> 'SUPERSEDED' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'a superseded admission opinion cannot change (FR-001)';
    END IF;
END$$

CREATE TRIGGER trg_admission_record_no_delete
    BEFORE DELETE
    ON admission_record
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'admission_record rows are removed only by the retention job (FR-051)';
    END IF;
END$$
DELIMITER ;

GRANT INSERT, UPDATE ON admission_record TO 'eldercare_app'@'%';
GRANT DELETE ON admission_record TO 'eldercare_retention'@'%';
