-- 011 T018 (US3; data-model mục 5, 0; FR-023 -> FR-032, BR-M08-06 -> 08): thực đơn tuần, nhóm 2.
-- Theo R-15: thuc_don_mon, thuc_don_mon_thay_the vào cột JSON content (MenuContent); kết quả kiểm tra (M08.MENU_CHECK) và
-- đổi món (M08.DISH_SWAP) ở record_event. Bữa, chế độ ăn, món (catalog_item) là tham chiếu logic, không khóa ngoại.
-- Cú pháp dùng được ở MySQL 8.0 và 8.4 (R-01).
CREATE TABLE menu
(
    id                         BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    week_start                 DATE          NOT NULL COMMENT 'Thứ Hai đầu tuần (FR-023)',
    status                     ENUM ('DRAFT', 'PENDING_PUBLICATION', 'PUBLISHED', 'APPLIED', 'CANCELLED') NOT NULL,
    content                    JSON          NOT NULL COMMENT 'MenuContent: ngày, bữa, chế độ, món, món thay thế',
    processing_check_confirmed BOOLEAN       NOT NULL DEFAULT FALSE COMMENT 'FR-025a, bắt buộc khi gửi',
    processing_note            VARCHAR(2000) NULL,
    processing_checked_by      BIGINT        NULL,
    processing_checked_at      DATETIME(6)   NULL,
    confirmed_warnings         JSON          NULL COMMENT 'Mã cảnh báo FR-025 đã xác nhận (bếp khi gửi, Bác sĩ khi công bố)',
    submitted_by               BIGINT        NULL,
    submitter_staff_id         BIGINT        NULL COMMENT 'Hồ sơ nhân viên người gửi, nhận thông báo trả lại, công bố',
    submitted_at               DATETIME(6)   NULL,
    published_by               BIGINT        NULL,
    published_at               DATETIME(6)   NULL,
    applied_at                 DATETIME(6)   NULL,
    return_reason              VARCHAR(2000) NULL,
    cancel_reason              VARCHAR(2000) NULL,
    copied_from_id             BIGINT        NULL,
    last_swap_id               BIGINT        NULL COMMENT 'record_event M08.DISH_SWAP gần nhất (FR-031)',
    version                    INT           NOT NULL DEFAULT 0,
    created_by                 BIGINT        NOT NULL,
    created_at                 DATETIME(6)   NOT NULL,
    active_week                DATE GENERATED ALWAYS AS (IF(status <> 'CANCELLED', week_start, NULL)) STORED
        COMMENT 'UNIQUE: tối đa một thực đơn chưa hủy mỗi tuần (FR-023)',
    CONSTRAINT uq_menu_active_week UNIQUE (active_week),
    CONSTRAINT ck_menu_week_start CHECK (WEEKDAY(week_start) = 0),
    CONSTRAINT ck_menu_content CHECK (JSON_TYPE(content) = 'OBJECT'),
    CONSTRAINT ck_menu_processing_check CHECK (status NOT IN ('PENDING_PUBLICATION', 'PUBLISHED', 'APPLIED')
        OR processing_check_confirmed),
    CONSTRAINT ck_menu_published CHECK (status NOT IN ('PUBLISHED', 'APPLIED') OR published_by IS NOT NULL),
    CONSTRAINT ck_menu_cancelled CHECK (status <> 'CANCELLED' OR cancel_reason IS NOT NULL),
    KEY ix_menu_status_week (status, week_start)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
    COMMENT = 'DataGroup=BUSINESS';

-- Nội dung sửa tự do khi Nháp. Thực đơn Công bố, Đã áp dụng chỉ đổi nội dung qua lệnh Đổi món: kèm id bản ghi đổi món mới
-- (FR-024, FR-031). Đã hủy là trạng thái cuối; Công bố, Đã áp dụng không về Nháp và không bị hủy (bảng trạng thái mục D).
DELIMITER $$
CREATE TRIGGER trg_menu_lock_content
    BEFORE UPDATE
    ON menu
    FOR EACH ROW
BEGIN
    IF OLD.status <> 'DRAFT' AND NOT (NEW.content <=> OLD.content) AND NOT (
        OLD.status IN ('PUBLISHED', 'APPLIED') AND NEW.status = OLD.status
            AND NEW.last_swap_id IS NOT NULL AND NOT (NEW.last_swap_id <=> OLD.last_swap_id)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'menu content is immutable outside DRAFT except by dish swap (FR-024, FR-031)';
    END IF;
    IF OLD.status = 'CANCELLED' AND NEW.status <> OLD.status THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'menu is in a final status (FR-023)';
    END IF;
    IF OLD.status IN ('PUBLISHED', 'APPLIED') AND NEW.status NOT IN ('PUBLISHED', 'APPLIED') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'published menu cannot go back or be cancelled (FR-023)';
    END IF;
END$$
DELIMITER ;

GRANT SELECT, INSERT, UPDATE ON menu TO 'eldercare_app'@'%';
GRANT DELETE ON menu TO 'eldercare_retention'@'%';
