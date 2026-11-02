-- 004 T051, T110: lượt vắng, báo vắng và ngày nghỉ bán trú (data-model mục 0, 8; FR-048 → FR-061, BR-M02-06, 07, 18; Q-395). Bảng gộp
-- theo cột kind (R-15). Bỏ ngày vắng, quyết định giữ giường, ngưỡng theo bảng hệ số. Lịch sử dự kiến trở lại ở status_history.
-- Cú pháp dùng được ở MySQL 8.0 và 8.4 (research R-01 của 000).

-- ABSENCE: lượt vắng, bất biến khi đã đóng (FR-061): sai sót về thời điểm đi đường đính chính M02.ABSENCE. Bệnh viện không bắt
-- buộc dự kiến trở lại (FR-048). DAY_CARE_NOTICE: báo vắng bán trú, nhóm 3, khóa ngay khi thêm (FR-060). CLOSURE_DAY: ngày khu
-- bán trú nghỉ, mỗi ngày tối đa một khai báo Hiệu lực, khóa khi hủy (FR-060a). Thời điểm lưu theo UTC.
CREATE TABLE absence
(
    id                  BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    kind                ENUM ('ABSENCE', 'DAY_CARE_NOTICE', 'CLOSURE_DAY') NOT NULL,
    resident_id         BIGINT        NULL COMMENT 'Rỗng ở ngày khu nghỉ',
    reason              VARCHAR(2000) NULL,
    status              ENUM ('OPEN', 'CLOSED', 'RECORDED', 'ACTIVE', 'CANCELLED') NOT NULL,
    created_by          BIGINT        NOT NULL,
    created_at          DATETIME(6)   NOT NULL,
    locked_at           DATETIME(6)   NULL,
    -- ABSENCE
    absence_type        ENUM ('HOME_VISIT', 'FAMILY_OUTING', 'SAME_DAY_MEDICAL_VISIT', 'OTHER', 'HOSPITAL') NULL,
    standard_reason     ENUM ('HOME_VISIT_END_OF_LIFE_WISH') NULL,
    departed_at         DATETIME(6)   NULL,
    expected_return_at  DATETIME(6)   NULL,
    returned_at         DATETIME(6)   NULL,
    picked_up_by_id     BIGINT        NULL,
    pickup_record_id    BIGINT        NULL,
    transfer_unit       VARCHAR(255)  NULL,
    handover_staff_id   BIGINT        NULL,
    takes_medication    BOOLEAN       NOT NULL DEFAULT FALSE,
    manager_notified_at DATETIME(6)   NULL COMMENT 'Đặt một lần khi tới ngày vắng thứ CFG-M02-05 (Q-395)',
    bed_released_at     DATETIME(6)   NULL COMMENT 'Lệnh Giải phóng giường; từ ngày này không tính phí lưu trú (FR-055)',
    bed_released_by     BIGINT        NULL,
    bed_release_reason  VARCHAR(2000) NULL,
    previous_absence_id BIGINT        NULL COMMENT 'Lượt Tạm vắng trước khi chuyển viện (FR-057)',
    closed_by           BIGINT        NULL,
    -- DAY_CARE_NOTICE, CLOSURE_DAY
    `date`              DATE          NULL,
    session_id          BIGINT        NULL COMMENT 'Rỗng là cả ngày; buổi thuộc 005 nên không có khóa ngoại',
    family_notified_at  DATETIME(6)   NULL,
    within_notice_period BOOLEAN      NULL,
    late_declaration    BOOLEAN       NULL,
    cancelled_by        BIGINT        NULL,
    cancel_reason       VARCHAR(2000) NULL,
    cancelled_at        DATETIME(6)   NULL,
    open_resident_id    BIGINT GENERATED ALWAYS AS (IF(kind = 'ABSENCE' AND status = 'OPEN', resident_id, NULL)) STORED,
    active_date         DATE GENERATED ALWAYS AS (IF(kind = 'CLOSURE_DAY' AND status = 'ACTIVE', `date`, NULL)) STORED,
    CONSTRAINT uq_absence_open_resident UNIQUE (open_resident_id),
    CONSTRAINT uq_absence_active_date UNIQUE (active_date),
    CONSTRAINT chk_absence_status CHECK (
        (kind = 'ABSENCE' AND status IN ('OPEN', 'CLOSED'))
            OR (kind = 'DAY_CARE_NOTICE' AND status = 'RECORDED')
            OR (kind = 'CLOSURE_DAY' AND status IN ('ACTIVE', 'CANCELLED'))),
    CONSTRAINT chk_absence_row CHECK (kind <> 'ABSENCE' OR (resident_id IS NOT NULL AND absence_type IS NOT NULL
        AND departed_at IS NOT NULL AND handover_staff_id IS NOT NULL)),
    CONSTRAINT chk_absence_expected_return CHECK (kind <> 'ABSENCE' OR absence_type = 'HOSPITAL' OR expected_return_at IS NOT NULL),
    CONSTRAINT chk_absence_closed CHECK (status <> 'CLOSED' OR returned_at IS NOT NULL),
    CONSTRAINT chk_absence_returned_after_departure CHECK (returned_at IS NULL OR returned_at >= departed_at),
    CONSTRAINT chk_absence_released CHECK (bed_released_at IS NULL OR (bed_released_by IS NOT NULL AND bed_release_reason IS NOT NULL)),
    CONSTRAINT chk_absence_notice CHECK (kind <> 'DAY_CARE_NOTICE' OR (resident_id IS NOT NULL AND `date` IS NOT NULL
        AND family_notified_at IS NOT NULL AND within_notice_period IS NOT NULL)),
    CONSTRAINT chk_absence_closure CHECK (kind <> 'CLOSURE_DAY' OR (`date` IS NOT NULL AND reason IS NOT NULL
        AND late_declaration IS NOT NULL)),
    CONSTRAINT fk_absence_previous FOREIGN KEY (previous_absence_id) REFERENCES absence (id) ON DELETE RESTRICT,
    KEY ix_absence_resident (kind, resident_id, departed_at),
    KEY ix_absence_open (kind, status, id),
    KEY ix_absence_date (kind, `date`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
    COMMENT = 'DataGroup=MULTIPLE';

-- Dùng USER() (tài khoản đang kết nối): trong trigger CURRENT_USER() là tài khoản tạo trigger. Thông báo bằng ASCII.
DELIMITER $$
-- FR-061: dòng đã khóa (lượt đã đóng, báo vắng, ngày nghỉ đã hủy) không bị sửa; lý do bản ghi giải phóng đặt một lần.
CREATE TRIGGER trg_absence_row_lock
    BEFORE UPDATE
    ON absence
    FOR EACH ROW
BEGIN
    IF OLD.locked_at IS NOT NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'absence row is locked, use a correction (FR-061)';
    END IF;
    IF OLD.bed_released_at IS NOT NULL AND NOT (NEW.bed_released_at <=> OLD.bed_released_at
        AND NEW.bed_released_by <=> OLD.bed_released_by AND NEW.bed_release_reason <=> OLD.bed_release_reason) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'bed release of an absence is set once (FR-055)';
    END IF;
    IF OLD.manager_notified_at IS NOT NULL AND NOT (NEW.manager_notified_at <=> OLD.manager_notified_at) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'manager notification of an absence is set once (FR-055)';
    END IF;
END$$

CREATE TRIGGER trg_absence_no_delete
    BEFORE DELETE
    ON absence
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'absence rows are removed only by the retention job';
    END IF;
END$$
DELIMITER ;

GRANT INSERT, UPDATE ON absence TO 'eldercare_app'@'%';
GRANT DELETE ON absence TO 'eldercare_retention'@'%';
