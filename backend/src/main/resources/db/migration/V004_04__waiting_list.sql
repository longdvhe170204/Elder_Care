-- 004 T023: danh sách chờ với điểm ưu tiên (data-model mục 4; FR-007 → FR-018, BR-M02-01, 02, 03, 10). Cú pháp dùng được
-- ở MySQL 8.0 và 8.4 (research R-01 của 000). Người cao tuổi, giường là định danh logic của 001, 003 nên không có khóa ngoại
-- sang module khác (constitution X); khóa ngoại tới giường chỉ thêm khi 003 vào develop (team-plan 3.5).

-- Hồ sơ chờ, nhóm 2. Tối đa một hồ sơ chờ chưa ở trạng thái cuối mỗi người (cột sinh open_resident_id có UNIQUE, FR-007) và
-- một giường chỉ do một hồ sơ giữ (held_bed_unique có UNIQUE, FR-014). Đã liên hệ thì bắt buộc có giường giữ.
CREATE TABLE waiting_list_entry
(
    id                      BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    application_id          BIGINT      NOT NULL,
    resident_id             BIGINT      NOT NULL,
    care_level              ENUM ('BASIC', 'REGULAR', 'SPECIAL') NOT NULL COMMENT 'Theo FR-008',
    care_level_not_assessed BOOLEAN     NOT NULL COMMENT 'Mức lấy từ lượt đăng ký vì chưa có đánh giá được chấp nhận',
    current_score           INT         NOT NULL,
    score_breakdown         JSON        NOT NULL COMMENT '{careLevel, waitingTime, circumstances, adjustment} (FR-009)',
    held_bed_id             BIGINT      NULL COMMENT 'Giường đang giữ chỗ; hạn giữ là dữ liệu của 003 (FR-014)',
    last_confirmed_at       DATETIME(6) NOT NULL COMMENT 'Mốc tính CFG-M02-03 (FR-017)',
    status                  ENUM ('WAITING', 'CONTACTED', 'ADMITTED', 'DECLINED', 'WAITING_CANCELLED') NOT NULL DEFAULT 'WAITING',
    open_resident_id        BIGINT GENERATED ALWAYS AS (IF(status IN ('WAITING', 'CONTACTED'), resident_id, NULL)) STORED
        COMMENT 'UNIQUE: tối đa một hồ sơ chờ chưa ở trạng thái cuối mỗi người (FR-007)',
    held_bed_unique         BIGINT GENERATED ALWAYS AS (IF(status = 'CONTACTED', held_bed_id, NULL)) STORED
        COMMENT 'UNIQUE: một giường chỉ do một hồ sơ giữ (FR-014)',
    version                 INT         NOT NULL DEFAULT 0,
    created_by              BIGINT      NOT NULL,
    created_at              DATETIME(6) NOT NULL,
    CONSTRAINT uq_waiting_list_entry_open UNIQUE (open_resident_id),
    CONSTRAINT uq_waiting_list_entry_held_bed UNIQUE (held_bed_unique),
    KEY ix_waiting_list_entry_application (application_id),
    KEY ix_waiting_list_entry_resident (resident_id, id),
    KEY ix_waiting_list_entry_status (status, current_score),
    CONSTRAINT fk_waiting_list_entry_application FOREIGN KEY (application_id) REFERENCES admission_application (id) ON DELETE RESTRICT,
    CONSTRAINT ck_waiting_list_entry_contacted_has_bed CHECK (status <> 'CONTACTED' OR held_bed_id IS NOT NULL)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Tình huống đặc biệt của hồ sơ chờ, nhóm 2. Mã nằm trong bảng CFG-M02-08 (kiểm ở dịch vụ vì bảng là tham số).
CREATE TABLE waiting_list_circumstance
(
    entry_id         BIGINT      NOT NULL,
    circumstance_code VARCHAR(60) NOT NULL,
    status           ENUM ('CONFIRMED', 'PENDING_CONFIRMATION') NOT NULL,
    PRIMARY KEY (entry_id, circumstance_code),
    CONSTRAINT fk_waiting_list_circumstance_entry FOREIGN KEY (entry_id) REFERENCES waiting_list_entry (id) ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Lịch sử điểm, nhóm 3 (chỉ thêm). Dòng DAILY chỉ thêm khi điểm đổi (FR-010); ADJUSTMENT trỏ yêu cầu phê duyệt (FR-011).
CREATE TABLE score_history
(
    id                  BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    entry_id            BIGINT      NOT NULL,
    old_score           INT         NOT NULL COMMENT '0 ở dòng CREATED',
    new_score           INT         NOT NULL,
    components          JSON        NOT NULL,
    cause               ENUM ('DAILY', 'CARE_LEVEL_CHANGED', 'CIRCUMSTANCES_CHANGED', 'ADJUSTMENT', 'SCORE_TABLE_CHANGED', 'CREATED') NOT NULL,
    approval_request_id BIGINT      NULL,
    at                  DATETIME(6) NOT NULL,
    KEY ix_score_history_entry (entry_id, id),
    CONSTRAINT fk_score_history_entry FOREIGN KEY (entry_id) REFERENCES waiting_list_entry (id) ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Lần liên hệ, từ chối, hết hạn giữ chỗ với một giường, nhóm 3 (chỉ thêm). Loại hồ sơ khỏi "lượt trống hiện tại" của giường
-- (FR-012) và truy vết công bằng khi chọn không theo thứ tự (BR-M02-10, FR-014). performed_by rỗng khi hệ thống ghi hết hạn.
CREATE TABLE bed_contact
(
    id                 BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    entry_id           BIGINT      NOT NULL,
    bed_id             BIGINT      NOT NULL,
    result             ENUM ('CONTACTED', 'BED_DECLINED', 'EXPIRED') NOT NULL,
    at                 DATETIME(6) NOT NULL,
    performed_by       BIGINT      NULL,
    reason             VARCHAR(1000) NULL,
    out_of_order_reason VARCHAR(1000) NULL,
    KEY ix_bed_contact_bed (bed_id, at),
    KEY ix_bed_contact_entry (entry_id, id),
    CONSTRAINT fk_bed_contact_entry FOREIGN KEY (entry_id) REFERENCES waiting_list_entry (id) ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Dùng USER() (tài khoản đang kết nối): trong trigger CURRENT_USER() là tài khoản tạo trigger. Thông báo bằng ASCII.
DELIMITER $$
-- Hồ sơ chờ ở trạng thái cuối (Đã tiếp nhận, Từ chối, Hủy chờ) không đổi; người, lượt đăng ký, dấu thời gian tạo bất biến.
CREATE TRIGGER trg_waiting_list_entry_guard
    BEFORE UPDATE
    ON waiting_list_entry
    FOR EACH ROW
BEGIN
    IF OLD.status IN ('ADMITTED', 'DECLINED', 'WAITING_CANCELLED') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'a closed waiting list entry cannot change (FR-007)';
    END IF;
    IF NOT (NEW.application_id <=> OLD.application_id
        AND NEW.resident_id <=> OLD.resident_id
        AND NEW.created_by <=> OLD.created_by
        AND NEW.created_at <=> OLD.created_at) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'application, resident and creation stamp of a waiting list entry are immutable';
    END IF;
END$$

CREATE TRIGGER trg_waiting_list_entry_no_delete
    BEFORE DELETE
    ON waiting_list_entry
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'waiting_list_entry rows are removed only by the retention job (FR-051)';
    END IF;
END$$

CREATE TRIGGER trg_score_history_no_delete
    BEFORE DELETE
    ON score_history
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'score_history rows are removed only by the retention job (DBR-23)';
    END IF;
END$$

CREATE TRIGGER trg_bed_contact_no_delete
    BEFORE DELETE
    ON bed_contact
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'bed_contact rows are removed only by the retention job (DBR-23)';
    END IF;
END$$
DELIMITER ;

CREATE TRIGGER trg_score_history_no_update
    BEFORE UPDATE
    ON score_history
    FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'score_history is append-only (DBR-23)';

CREATE TRIGGER trg_bed_contact_no_update
    BEFORE UPDATE
    ON bed_contact
    FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'bed_contact is append-only (DBR-23)';

GRANT INSERT, UPDATE ON waiting_list_entry TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE, DELETE ON waiting_list_circumstance TO 'eldercare_app'@'%';
GRANT INSERT ON score_history TO 'eldercare_app'@'%';
GRANT INSERT ON bed_contact TO 'eldercare_app'@'%';
GRANT DELETE ON waiting_list_entry TO 'eldercare_retention'@'%';
GRANT DELETE ON waiting_list_circumstance TO 'eldercare_retention'@'%';
GRANT DELETE ON score_history TO 'eldercare_retention'@'%';
GRANT DELETE ON bed_contact TO 'eldercare_retention'@'%';
