-- 009 T018 (tên logic V009_02__nhac_goi; số 02 đã dùng cho thiết bị đẩy): lượt nhắc gọi (data-model mục 6, research
-- B-08; FR-027 → FR-032). Lượt, thông báo gộp, chuyển nhắc, kết quả gọi là nhóm 3 (chỉ thêm; kết quả gọi sửa qua đính
-- chính của 000, loại M13.CALL_RESULT); trạng thái lượt là bảng kỹ thuật dẫn xuất, khóa pending_key giữ tối đa một
-- lượt Chờ kết quả mỗi (người cần gọi, người cao tuổi), không gắn người cao tuổi thì theo bản ghi nguồn (FR-032, Q-100).

CREATE TABLE call_reminder
(
    id                 BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    call_chain_id      BIGINT      NULL COMMENT 'Lượt đầu chuỗi gọi; NULL: chính lượt này là đầu chuỗi (FR-029a)',
    callee_type        ENUM ('STAFF', 'FAMILY_MEMBER') NOT NULL,
    callee_id          BIGINT      NOT NULL,
    resident_id        BIGINT      NULL,
    source_record_type VARCHAR(64) NULL COMMENT 'Khi không gắn người cao tuổi (FR-027a)',
    source_record_id   VARCHAR(64) NULL,
    floor_area_id      BIGINT      NULL,
    creation_reason    ENUM ('UNACKNOWLEDGED', 'DELIVERY_FAILED', 'PREVIOUS_NOT_REACHED') NOT NULL,
    created_at         DATETIME(6) NOT NULL,
    CONSTRAINT fk_call_reminder_chain FOREIGN KEY (call_chain_id) REFERENCES call_reminder (id),
    KEY idx_call_reminder_resident (resident_id, created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Thông báo gộp vào lượt (FR-027a): mỗi thông báo thuộc tối đa một lượt (FR-032).
CREATE TABLE call_reminder_notification
(
    id               BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    call_reminder_id BIGINT      NOT NULL,
    notification_id  BIGINT      NOT NULL,
    merged_at        DATETIME(6) NOT NULL,
    CONSTRAINT uq_call_reminder_notification UNIQUE (notification_id),
    CONSTRAINT fk_call_reminder_notification_reminder FOREIGN KEY (call_reminder_id) REFERENCES call_reminder (id),
    CONSTRAINT fk_call_reminder_notification_notification FOREIGN KEY (notification_id) REFERENCES notification (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Chuyển nhắc (FR-030a): người được nhắc trước, sau, lý do, hạn = thời điểm + CFG-M13-04.
CREATE TABLE call_reminder_handoff
(
    id                 BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    call_reminder_id   BIGINT      NOT NULL,
    previous_staff_id  BIGINT      NULL,
    reminded_staff_id  BIGINT      NOT NULL,
    tier               ENUM ('SHIFT_LEAD', 'FLOOR_MANAGER', 'MANAGERS') NOT NULL COMMENT 'Bậc FR-029 của người được nhắc',
    reason             ENUM ('INITIAL', 'OVERDUE', 'SHIFT_ENDED', 'CONTENT_UPDATED', 'MANAGER_REPEAT') NOT NULL,
    due_at             DATETIME(6) NOT NULL,
    occurred_at        DATETIME(6) NOT NULL,
    CONSTRAINT fk_call_reminder_handoff_reminder FOREIGN KEY (call_reminder_id) REFERENCES call_reminder (id),
    KEY idx_call_reminder_handoff_reminder (call_reminder_id, occurred_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Trạng thái lượt, kỹ thuật dẫn xuất (FR-030): cập nhật cùng transaction với kết quả gọi, chuyển nhắc, xác nhận.
CREATE TABLE call_reminder_state
(
    call_reminder_id  BIGINT       NOT NULL PRIMARY KEY,
    status            ENUM ('AWAITING_RESULT', 'RESULT_RECORDED', 'STOPPED') NOT NULL,
    final_result      ENUM ('REACHED', 'NOT_REACHED') NULL,
    stop_reason       VARCHAR(100) NULL,
    due_at            DATETIME(6)  NOT NULL,
    current_staff_id  BIGINT       NOT NULL COMMENT 'Người được nhắc hiện tại',
    callee_key        VARCHAR(191) NOT NULL COMMENT 'loại:id người cần gọi:resident:id hoặc source:loại:id',
    pending_key       VARCHAR(191) AS (IF(status = 'AWAITING_RESULT', callee_key, NULL)) STORED,
    CONSTRAINT uq_call_reminder_state_pending UNIQUE (pending_key),
    CONSTRAINT fk_call_reminder_state_reminder FOREIGN KEY (call_reminder_id) REFERENCES call_reminder (id),
    KEY idx_call_reminder_state_due (status, due_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Kết quả gọi (FR-030, FR-031), nhóm 3: Không liên lạc được bắt buộc lý do, Đã liên lạc được bắt buộc ghi chú.
CREATE TABLE call_result
(
    id                  BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    call_reminder_id    BIGINT        NOT NULL,
    called_by           BIGINT        NOT NULL COMMENT 'Tài khoản người gọi',
    called_at           DATETIME(6)   NOT NULL,
    called_phone_number VARCHAR(30)   NULL,
    result              ENUM ('REACHED', 'NOT_REACHED') NOT NULL,
    reason              ENUM ('NO_ANSWER', 'WRONG_NUMBER', 'NO_NUMBER', 'OTHER') NULL,
    answered_by         VARCHAR(200)  NULL,
    note                VARCHAR(2000) NULL,
    supplementary       BOOLEAN       NOT NULL DEFAULT FALSE COMMENT 'Lần gọi bổ sung sau Không liên lạc được (FR-029a)',
    recorded_at         DATETIME(6)   NOT NULL,
    CONSTRAINT chk_call_result_reason CHECK (result <> 'NOT_REACHED' OR reason IS NOT NULL),
    CONSTRAINT chk_call_result_note CHECK (result <> 'REACHED' OR note IS NOT NULL),
    CONSTRAINT fk_call_result_reminder FOREIGN KEY (call_reminder_id) REFERENCES call_reminder (id),
    KEY idx_call_result_reminder (call_reminder_id, recorded_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

DELIMITER $$
CREATE TRIGGER trg_call_reminder_no_update BEFORE UPDATE ON call_reminder FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'call_reminder is append-only';
END$$
CREATE TRIGGER trg_call_reminder_no_delete BEFORE DELETE ON call_reminder FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'call_reminder rows are removed only by the retention job';
    END IF;
END$$
CREATE TRIGGER trg_call_reminder_notification_no_update BEFORE UPDATE ON call_reminder_notification FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'call_reminder_notification is append-only';
END$$
CREATE TRIGGER trg_call_reminder_notification_no_delete BEFORE DELETE ON call_reminder_notification FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'call_reminder_notification rows are removed only by the retention job';
    END IF;
END$$
CREATE TRIGGER trg_call_reminder_handoff_no_update BEFORE UPDATE ON call_reminder_handoff FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'call_reminder_handoff is append-only';
END$$
CREATE TRIGGER trg_call_reminder_handoff_no_delete BEFORE DELETE ON call_reminder_handoff FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'call_reminder_handoff rows are removed only by the retention job';
    END IF;
END$$
CREATE TRIGGER trg_call_result_no_update BEFORE UPDATE ON call_result FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'call_result is append-only';
END$$
CREATE TRIGGER trg_call_result_no_delete BEFORE DELETE ON call_result FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'call_result rows are removed only by the retention job';
    END IF;
END$$
DELIMITER ;

GRANT INSERT ON call_reminder TO 'eldercare_app'@'%';
GRANT INSERT ON call_reminder_notification TO 'eldercare_app'@'%';
GRANT INSERT ON call_reminder_handoff TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE ON call_reminder_state TO 'eldercare_app'@'%';
GRANT INSERT ON call_result TO 'eldercare_app'@'%';
GRANT DELETE ON call_reminder TO 'eldercare_retention'@'%';
GRANT DELETE ON call_reminder_notification TO 'eldercare_retention'@'%';
GRANT DELETE ON call_reminder_handoff TO 'eldercare_retention'@'%';
GRANT DELETE ON call_reminder_state TO 'eldercare_retention'@'%';
GRANT DELETE ON call_result TO 'eldercare_retention'@'%';
