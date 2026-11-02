-- 004 T058, T063, T110: hồ sơ kết thúc lưu trú và bản ghi qua đời (data-model mục 0, 9, 10; FR-062 → FR-073, BR-M02-21). Bảng gộp theo
-- cột kind (R-15). Bàn giao, ngoại lệ điều kiện ở record_event; danh sách việc sau qua đời ở resident_module_state. Cú pháp
-- dùng được ở MySQL 8.0 và 8.4 (research R-01 của 000).

-- ENDING: một người chỉ có một hồ sơ Đang chuẩn bị (open_resident_id UNIQUE, FR-062); hồ sơ Hoàn tất hoặc Đã hủy khóa (FR-067).
-- DEATH: mỗi người một bản ghi (death_resident_id UNIQUE), nhóm 3, khóa ngay khi thêm; bổ sung nguyên nhân đi đính chính
-- M02.DEATH_RECORD. cause_of_death thuộc nhóm thông tin sức khỏe (FR-069, BR-M02-21).
CREATE TABLE residency_ending
(
    id                BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    kind              ENUM ('ENDING', 'DEATH') NOT NULL,
    resident_id       BIGINT        NOT NULL,
    created_by        BIGINT        NOT NULL,
    created_at        DATETIME(6)   NOT NULL,
    locked_at         DATETIME(6)   NULL,
    version           INT           NOT NULL DEFAULT 0,
    -- ENDING
    end_case          ENUM ('CONTRACT_EXPIRED', 'DISCHARGE', 'TRANSFER_TO_ANOTHER_FACILITY', 'CONTRACT_TERMINATION') NULL,
    medical_reason    BOOLEAN       NOT NULL DEFAULT FALSE,
    destination       VARCHAR(255)  NULL,
    expected_end_date DATE          NULL,
    actual_ended_at   DATETIME(6)   NULL COMMENT 'Thời điểm hiệu lực của lệnh Kết thúc lưu trú (FR-065)',
    requested_by      VARCHAR(255)  NULL,
    reason            VARCHAR(2000) NULL,
    overdue_since     DATE          NULL COMMENT 'Dấu "quá ngày dự kiến" từ ngày liền sau ngày kết thúc dự kiến (FR-066a)',
    final_period_ref  BIGINT        NULL COMMENT 'Bảng kỳ cuối nháp do KyCuoiPort của 010 trả (FR-066)',
    status            ENUM ('PREPARING', 'COMPLETED', 'CANCELLED') NULL,
    cancel_reason     VARCHAR(2000) NULL,
    closed_by         BIGINT        NULL,
    closed_at         DATETIME(6)   NULL,
    -- DEATH
    death_at          DATETIME(6)   NULL,
    place             VARCHAR(255)  NULL,
    discovered_by     VARCHAR(255)  NULL,
    confirmed_by_id   BIGINT        NULL,
    cause_of_death    VARCHAR(2000) NULL,
    evidence_file_id  BIGINT        NULL,
    history_id        BIGINT        NULL COMMENT 'Lịch sử trạng thái của lệnh Ghi nhận qua đời (001)',
    open_resident_id  BIGINT GENERATED ALWAYS AS (IF(kind = 'ENDING' AND status = 'PREPARING', resident_id, NULL)) STORED,
    death_resident_id BIGINT GENERATED ALWAYS AS (IF(kind = 'DEATH', resident_id, NULL)) STORED,
    CONSTRAINT uq_residency_ending_open_resident UNIQUE (open_resident_id),
    CONSTRAINT uq_residency_ending_death_resident UNIQUE (death_resident_id),
    CONSTRAINT chk_residency_ending_row CHECK (kind <> 'ENDING' OR (end_case IS NOT NULL AND expected_end_date IS NOT NULL
        AND requested_by IS NOT NULL AND reason IS NOT NULL AND status IS NOT NULL)),
    CONSTRAINT chk_residency_ending_death CHECK (kind <> 'DEATH' OR (death_at IS NOT NULL AND place IS NOT NULL
        AND discovered_by IS NOT NULL AND confirmed_by_id IS NOT NULL AND history_id IS NOT NULL AND status IS NULL)),
    CONSTRAINT chk_residency_ending_destination CHECK (end_case IS NULL OR end_case <> 'TRANSFER_TO_ANOTHER_FACILITY'
        OR destination IS NOT NULL),
    CONSTRAINT chk_residency_ending_completed CHECK (status IS NULL OR status <> 'COMPLETED' OR actual_ended_at IS NOT NULL),
    CONSTRAINT chk_residency_ending_cancelled CHECK (status IS NULL OR status <> 'CANCELLED' OR cancel_reason IS NOT NULL),
    KEY ix_residency_ending_resident (kind, resident_id, created_at),
    KEY ix_residency_ending_open (kind, status, expected_end_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
    COMMENT = 'DataGroup=MULTIPLE';

-- Dùng USER() (tài khoản đang kết nối): trong trigger CURRENT_USER() là tài khoản tạo trigger. Thông báo bằng ASCII.
DELIMITER $$
CREATE TRIGGER trg_residency_ending_row_lock
    BEFORE UPDATE
    ON residency_ending
    FOR EACH ROW
BEGIN
    IF OLD.locked_at IS NOT NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'a closed residency_ending row cannot be changed (FR-067, FR-069)';
    END IF;
END$$

CREATE TRIGGER trg_residency_ending_no_delete
    BEFORE DELETE
    ON residency_ending
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'residency_ending rows are removed only by the retention job';
    END IF;
END$$
DELIMITER ;

GRANT INSERT, UPDATE ON residency_ending TO 'eldercare_app'@'%';
GRANT DELETE ON residency_ending TO 'eldercare_retention'@'%';
