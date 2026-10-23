-- 009 T025: kênh ngoài ứng dụng (data-model mục 6, 9; research B-05). Khung gộp tin nhắn Trung bình (kỹ thuật), danh
-- sách "cần liên hệ trực tiếp" (nhóm 2: chỉ đổi trạng thái từ Chờ liên hệ), kết quả liên hệ (nhóm 3, một kết quả mỗi
-- mục, là trạng thái cuối; sửa qua đính chính của 000). Dấu nội dung rút gọn không hợp lệ cho kênh ngoài (FR-020), ghi
-- lúc tiếp nhận vì tham số hiển thị không lưu, dùng lại khi giải phóng giờ yên tĩnh.

ALTER TABLE notification_request
    MODIFY flags SET ('MISSING_LEVEL', 'MISSING_INFORMATION_TYPE', 'MISSING_SHORT_CONTENT', 'NO_RECIPIENT',
                      'AFTER_DEATH', 'MISSING_INFORMATION_SUBJECT', 'MISSING_Q354_CASE',
                      'INVALID_OUT_OF_APP_CONTENT') NOT NULL DEFAULT '';

-- Lần gửi: khung gộp mà tin dồn vào (chờ tin tổng hợp), nội dung riêng của lần gửi (tin tổng hợp; gửi lại dùng lại).
ALTER TABLE delivery_attempt
    ADD COLUMN batch_window_id BIGINT       NULL COMMENT 'Khung gộp tin dồn vào (FR-016b)',
    ADD COLUMN content         VARCHAR(500) NULL COMMENT 'Nội dung riêng; NULL thì dùng nội dung rút gọn của yêu cầu',
    ADD KEY idx_delivery_attempt_window (batch_window_id);

-- Khung gộp (FR-016b): một khung mở mỗi người nhận (open_key); first_attempt_id là tin mở khung (gửi ngay hoặc tin
-- tổng hợp của khung trước). Đóng khung ghi lần gửi tin tổng hợp (nếu có tin dồn).
CREATE TABLE sms_batch_window
(
    id                BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    recipient_key     VARCHAR(40)  NOT NULL COMMENT 'loại:id người nhận như notification_state',
    started_at        DATETIME(6)  NOT NULL,
    ends_at           DATETIME(6)  NOT NULL,
    first_attempt_id  BIGINT       NOT NULL,
    closed_at         DATETIME(6)  NULL,
    digest_attempt_id BIGINT       NULL,
    open_key          VARCHAR(40) AS (IF(closed_at IS NULL, recipient_key, NULL)) STORED,
    CONSTRAINT uq_sms_batch_window_open UNIQUE (open_key),
    CONSTRAINT uq_sms_batch_window_digest UNIQUE (digest_attempt_id),
    KEY idx_sms_batch_window_due (closed_at, ends_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Cần liên hệ trực tiếp (FR-021), nhóm 2.
CREATE TABLE direct_contact
(
    id               BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    notification_id  BIGINT        NOT NULL,
    family_member_id BIGINT        NULL,
    full_name        VARCHAR(200)  NULL,
    relationship     VARCHAR(100)  NULL,
    phone_number     VARCHAR(30)   NULL,
    short_content    VARCHAR(1000) NULL,
    status           ENUM ('PENDING', 'CONTACTED', 'UNREACHABLE') NOT NULL DEFAULT 'PENDING',
    created_at       DATETIME(6)   NOT NULL,
    reminded_at      DATETIME(6)   NULL,
    version          BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uq_direct_contact_notification UNIQUE (notification_id),
    CONSTRAINT fk_direct_contact_notification FOREIGN KEY (notification_id) REFERENCES notification (id),
    KEY idx_direct_contact_status (status, created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Kết quả liên hệ (FR-021, FR-031), nhóm 3: Không liên hệ được bắt buộc lý do.
CREATE TABLE direct_contact_result
(
    id                BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    direct_contact_id BIGINT        NOT NULL,
    contacted_by      BIGINT        NOT NULL COMMENT 'Tài khoản Hành chính',
    occurred_at       DATETIME(6)   NOT NULL,
    result            ENUM ('CONTACTED', 'UNREACHABLE') NOT NULL,
    reason            VARCHAR(2000) NULL,
    note              VARCHAR(2000) NULL,
    recorded_at       DATETIME(6)   NOT NULL,
    CONSTRAINT uq_direct_contact_result UNIQUE (direct_contact_id),
    CONSTRAINT chk_direct_contact_result_reason CHECK (result <> 'UNREACHABLE' OR reason IS NOT NULL),
    CONSTRAINT fk_direct_contact_result_contact FOREIGN KEY (direct_contact_id) REFERENCES direct_contact (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

DELIMITER $$
CREATE TRIGGER trg_direct_contact_final BEFORE UPDATE ON direct_contact FOR EACH ROW
BEGIN
    IF OLD.status <> 'PENDING' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'direct_contact is final once contacted or unreachable';
    END IF;
END$$
CREATE TRIGGER trg_direct_contact_no_delete BEFORE DELETE ON direct_contact FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'direct_contact rows are removed only by the retention job';
    END IF;
END$$
CREATE TRIGGER trg_direct_contact_result_no_update BEFORE UPDATE ON direct_contact_result FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'direct_contact_result is append-only';
END$$
CREATE TRIGGER trg_direct_contact_result_no_delete BEFORE DELETE ON direct_contact_result FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'direct_contact_result rows are removed only by the retention job';
    END IF;
END$$
DELIMITER ;

GRANT INSERT, UPDATE ON sms_batch_window TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE ON direct_contact TO 'eldercare_app'@'%';
GRANT INSERT ON direct_contact_result TO 'eldercare_app'@'%';
GRANT DELETE ON sms_batch_window TO 'eldercare_retention'@'%';
GRANT DELETE ON direct_contact TO 'eldercare_retention'@'%';
GRANT DELETE ON direct_contact_result TO 'eldercare_retention'@'%';
