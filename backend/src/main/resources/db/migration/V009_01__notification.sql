-- 009 T006: kênh, yêu cầu thông báo, thông báo, sự kiện, lần gửi, thiết bị đẩy, lần chạy job (data-model mục 2 → 5;
-- research B-02 → B-05, B-09, B-10). Bản ghi nguồn, người cao tuổi, tầng/khu vực, nhân viên, người thân, tài khoản là
-- tham chiếu logic (BIGINT, không khóa ngoại sang module khác). Bảng nhóm 3 chỉ thêm: hai trigger chặn sửa, xóa (xóa
-- chỉ bằng tài khoản eldercare_retention); bảng kỹ thuật dẫn xuất được sửa (như correction_chain của 000).

-- Kênh thông báo, nhóm 1 (FR-016, FR-039, FR-040). levels chỉ dùng cho kênh khác (OTHER_*): mức được gắn.
CREATE TABLE notification_channel
(
    code    VARCHAR(40)                       NOT NULL PRIMARY KEY COMMENT 'IN_APP, FAMILY_PORTAL, SMS, OTHER_*',
    name    VARCHAR(200)                      NOT NULL,
    enabled BOOLEAN                           NOT NULL,
    levels  SET ('LOW', 'MEDIUM', 'URGENT')   NOT NULL DEFAULT 'URGENT',
    config  JSON                              NULL COMMENT 'Thông tin nhà cung cấp; không chứa bí mật',
    version BIGINT                            NOT NULL DEFAULT 0
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Tin nhắn mặc định chưa bật (Q-06: giai đoạn đầu chỉ trong ứng dụng; Quản lý viện bật khi có nhà cung cấp).
INSERT INTO notification_channel (code, name, enabled)
VALUES ('IN_APP', 'Trong ứng dụng', TRUE),
       ('FAMILY_PORTAL', 'Cổng người thân', TRUE),
       ('SMS', 'Tin nhắn', FALSE);

-- Yêu cầu thông báo, nhóm 3 (FR-001 → FR-005). Yêu cầu bị từ chối vẫn lưu (FR-003), có thể thiếu bản ghi nguồn, khóa.
CREATE TABLE notification_request
(
    id                  BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    source_module       VARCHAR(20)   NULL,
    event_type          VARCHAR(100)  NULL,
    source_group        VARCHAR(30)   NULL,
    source_record_type  VARCHAR(64)   NULL,
    source_record_id    VARCHAR(64)   NULL,
    event_key           VARCHAR(191)  NULL,
    level               ENUM ('LOW', 'MEDIUM', 'URGENT') NOT NULL COMMENT 'Thiếu thì LOW và dấu MISSING_LEVEL',
    resident_id         BIGINT        NULL,
    floor_area_id       BIGINT        NULL,
    short_content       VARCHAR(1000) NULL,
    title               VARCHAR(500)  NULL,
    caused_by           BIGINT        NULL,
    notify_causer       BOOLEAN       NOT NULL DEFAULT FALSE,
    occurred_at         DATETIME(6)   NULL,
    received_at         DATETIME(6)   NOT NULL,
    intake_result       ENUM ('VALID', 'REJECTED') NOT NULL,
    rejection_reason    VARCHAR(100)  NULL,
    flags               SET ('MISSING_LEVEL', 'MISSING_INFORMATION_TYPE', 'MISSING_SHORT_CONTENT', 'NO_RECIPIENT',
                             'AFTER_DEATH', 'MISSING_INFORMATION_SUBJECT', 'MISSING_Q354_CASE') NOT NULL DEFAULT '',
    information_subject ENUM ('ABOUT_RESIDENT', 'ABOUT_FAMILY_MATTER') NULL,
    q354_case           ENUM ('PRIMARY_CONTACT_EMERGENCY', 'CONTACT_LIST', 'WISH_RECONFIRMATION', 'TRANSFER', 'DEATH') NULL,
    CONSTRAINT uq_notification_request_key UNIQUE (source_record_type, source_record_id, event_key),
    CONSTRAINT chk_notification_request_rejection CHECK ((intake_result = 'REJECTED') = (rejection_reason IS NOT NULL)),
    KEY idx_notification_request_floor (floor_area_id, received_at),
    KEY idx_notification_request_result (intake_result, received_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Lần nhận trùng (FR-005): kỹ thuật, một dòng mỗi lần; không sửa dòng yêu cầu.
CREATE TABLE notification_request_duplicate
(
    id          BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    request_id  BIGINT      NOT NULL,
    received_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_notification_request_duplicate_request FOREIGN KEY (request_id) REFERENCES notification_request (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Phần nội dung của yêu cầu, nhóm 3 (FR-013): thiếu loại thông tin thì coi là HEALTH.
CREATE TABLE notification_request_content
(
    id               BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    request_id       BIGINT        NOT NULL,
    seq              INT           NOT NULL,
    information_type ENUM ('GENERAL', 'HEALTH', 'COSTS', 'VISITS', 'CONTRACT') NOT NULL,
    content          VARCHAR(4000) NOT NULL,
    CONSTRAINT uq_notification_request_content UNIQUE (request_id, seq),
    CONSTRAINT fk_notification_request_content_request FOREIGN KEY (request_id) REFERENCES notification_request (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Nhóm người nhận của yêu cầu, nhóm 3 (research B-04): mã biến thể RecipientGroup, tham số, cách thay, kết quả.
CREATE TABLE notification_request_recipient_group
(
    id                BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    request_id        BIGINT      NOT NULL,
    seq               INT         NOT NULL,
    group_code        VARCHAR(40) NOT NULL,
    params            JSON        NOT NULL,
    substitution      ENUM ('DEFAULT', 'NO_SUBSTITUTE', 'DESIGNATED') NULL COMMENT 'Chỉ nhóm loại (a)',
    substitute_group  JSON        NULL,
    outcome           ENUM ('HAS_RECIPIENTS', 'SUBSTITUTED', 'SKIPPED_NO_SUBSTITUTE', 'NONE') NULL,
    CONSTRAINT uq_notification_request_recipient_group UNIQUE (request_id, seq),
    CONSTRAINT fk_notification_request_recipient_group_request FOREIGN KEY (request_id) REFERENCES notification_request (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Thông báo, nhóm 3: một dòng mỗi người nhận mỗi yêu cầu (FR-012).
CREATE TABLE notification
(
    id             BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    request_id     BIGINT      NOT NULL,
    recipient_type ENUM ('STAFF', 'FAMILY_MEMBER', 'RESIDENT') NOT NULL,
    recipient_id   BIGINT      NOT NULL,
    account_id     BIGINT      NULL,
    basis          JSON        NOT NULL COMMENT 'Mọi căn cứ (FR-009 → FR-012)',
    visible_parts  SET ('GENERAL', 'HEALTH', 'COSTS', 'VISITS', 'CONTRACT') NOT NULL DEFAULT 'GENERAL',
    identity_only  BOOLEAN     NOT NULL DEFAULT FALSE COMMENT 'Nhận thay không có phạm vi (FR-011)',
    q354_case      ENUM ('PRIMARY_CONTACT_EMERGENCY', 'CONTACT_LIST', 'WISH_RECONFIRMATION', 'TRANSFER', 'DEATH') NULL
        COMMENT 'Mã Q-354 áp dụng cho người nhận ngoài phạm vi bản đồng ý (FR-014a)',
    level          ENUM ('LOW', 'MEDIUM', 'URGENT') NOT NULL,
    created_at     DATETIME(6) NOT NULL,
    CONSTRAINT uq_notification_recipient UNIQUE (request_id, recipient_type, recipient_id),
    CONSTRAINT fk_notification_request FOREIGN KEY (request_id) REFERENCES notification_request (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Sự kiện của thông báo, nhóm 3 (FR-044, research B-09). once_key: xem lần đầu, xác nhận, gửi, không gửi chỉ một lần.
CREATE TABLE notification_event
(
    id                     BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    notification_id        BIGINT       NOT NULL,
    type                   ENUM ('HOLD', 'RESCHEDULE', 'SEND', 'NOT_SENT', 'VIEW', 'ACKNOWLEDGE', 'UNREACHABLE', 'HIDE') NOT NULL,
    acknowledgement_method ENUM ('IN_APP', 'BY_CALL', 'LATE', 'LATE_BY_CALL') NULL,
    reason                 VARCHAR(200) NULL,
    scheduled_send_at      DATETIME(6)  NULL COMMENT 'Với HOLD, RESCHEDULE',
    actor_id               BIGINT       NULL COMMENT 'NULL: Hệ thống',
    occurred_at            DATETIME(6)  NOT NULL,
    once_key               VARCHAR(20) AS (IF(type IN ('SEND', 'NOT_SENT', 'VIEW', 'ACKNOWLEDGE'), type, NULL)) STORED,
    CONSTRAINT uq_notification_event_once UNIQUE (notification_id, once_key),
    CONSTRAINT chk_notification_event_ack CHECK ((type = 'ACKNOWLEDGE') = (acknowledgement_method IS NOT NULL)),
    CONSTRAINT chk_notification_event_schedule CHECK (type NOT IN ('HOLD', 'RESCHEDULE') OR scheduled_send_at IS NOT NULL),
    CONSTRAINT fk_notification_event_notification FOREIGN KEY (notification_id) REFERENCES notification (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Trạng thái hiện hành, kỹ thuật dẫn xuất (research B-09): cập nhật cùng transaction với notification_event,
-- delivery_result; luôn bằng hàm thuần NotificationStatusRules trên sự kiện.
CREATE TABLE notification_state
(
    notification_id        BIGINT      NOT NULL PRIMARY KEY,
    status                 ENUM ('HELD_FOR_QUIET_HOURS', 'NOT_SENT', 'SENT', 'DELIVERY_FAILED', 'VIEWED', 'ACKNOWLEDGED',
                                 'UNREACHABLE') NULL COMMENT 'NULL: chưa có lần gửi nào xong',
    scheduled_send_at      DATETIME(6) NULL,
    sent_at                DATETIME(6) NULL,
    viewed_at              DATETIME(6) NULL,
    acknowledged_at        DATETIME(6) NULL,
    acknowledgement_method ENUM ('IN_APP', 'BY_CALL', 'LATE', 'LATE_BY_CALL') NULL,
    hidden                 BOOLEAN     NOT NULL DEFAULT FALSE,
    recipient_key          VARCHAR(40) NOT NULL COMMENT 'loại:id người nhận, để lọc hộp thông báo',
    level                  ENUM ('LOW', 'MEDIUM', 'URGENT') NOT NULL,
    resident_id            BIGINT      NULL,
    CONSTRAINT fk_notification_state_notification FOREIGN KEY (notification_id) REFERENCES notification (id),
    KEY idx_notification_state_inbox (recipient_key, sent_at),
    KEY idx_notification_state_due (status, scheduled_send_at),
    KEY idx_notification_state_level (level, status, sent_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Lần gửi theo kênh, nhóm 3 (FR-018): attempt_no = 1 + số lần gửi lại; gửi lại không trùng (NFR-04).
CREATE TABLE delivery_attempt
(
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    notification_id BIGINT       NOT NULL,
    channel         VARCHAR(40)  NOT NULL,
    address         VARCHAR(500) NULL COMMENT 'Số điện thoại, token; hiển thị che một phần',
    attempt_no      INT          NOT NULL,
    scheduled_at    DATETIME(6)  NOT NULL,
    CONSTRAINT uq_delivery_attempt UNIQUE (notification_id, channel, attempt_no),
    CONSTRAINT fk_delivery_attempt_notification FOREIGN KEY (notification_id) REFERENCES notification (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Kết quả lần gửi, nhóm 3: một kết quả mỗi lần gửi.
CREATE TABLE delivery_result
(
    id                BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    attempt_id        BIGINT       NOT NULL,
    result            ENUM ('SENT', 'FAILED', 'CHANNEL_NOT_CONFIGURED', 'NO_ADDRESS', 'SKIPPED_OFF_SHIFT', 'BATCHED',
                            'INVALID_CONTENT') NOT NULL,
    reason            VARCHAR(500) NULL,
    digest_attempt_id BIGINT       NULL COMMENT 'Lần gửi tin tổng hợp, với BATCHED',
    occurred_at       DATETIME(6)  NOT NULL,
    CONSTRAINT uq_delivery_result_attempt UNIQUE (attempt_id),
    CONSTRAINT fk_delivery_result_attempt FOREIGN KEY (attempt_id) REFERENCES delivery_attempt (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Token FCM theo tài khoản và mã thiết bị của 002 (research B-05), kỹ thuật. Token lỗi thì đặt invalidated_at.
CREATE TABLE push_device
(
    account_id     BIGINT       NOT NULL,
    device_id      CHAR(36)     NOT NULL,
    app            ENUM ('STAFF_APP', 'FAMILY_APP') NOT NULL,
    fcm_token      VARCHAR(512) NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    invalidated_at DATETIME(6)  NULL,
    PRIMARY KEY (account_id, device_id),
    KEY idx_push_device_token (fcm_token(191))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Ngày đã chạy xong của job hằng ngày M13, để chạy bù khi khởi động (research B-10, NFR-04), kỹ thuật.
CREATE TABLE m13_job_run
(
    job              VARCHAR(60) NOT NULL,
    business_date    DATE        NOT NULL,
    scheduler_run_id BIGINT      NOT NULL,
    completed_at     DATETIME(6) NOT NULL,
    PRIMARY KEY (job, business_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Trigger chặn sửa, xóa của bảng nhóm 3 (mẫu 000 data-model mục 1, DBR-23).
CREATE TRIGGER trg_notification_request_no_update BEFORE UPDATE ON notification_request FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'notification_request is append-only';
CREATE TRIGGER trg_notification_request_content_no_update BEFORE UPDATE ON notification_request_content FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'notification_request_content is append-only';
CREATE TRIGGER trg_notification_request_recipient_group_no_update BEFORE UPDATE ON notification_request_recipient_group FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'notification_request_recipient_group is append-only';
CREATE TRIGGER trg_notification_no_update BEFORE UPDATE ON notification FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'notification is append-only';
CREATE TRIGGER trg_notification_event_no_update BEFORE UPDATE ON notification_event FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'notification_event is append-only';
CREATE TRIGGER trg_delivery_attempt_no_update BEFORE UPDATE ON delivery_attempt FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'delivery_attempt is append-only';
CREATE TRIGGER trg_delivery_result_no_update BEFORE UPDATE ON delivery_result FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'delivery_result is append-only';

DELIMITER $$
CREATE TRIGGER trg_notification_request_no_delete BEFORE DELETE ON notification_request FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'notification_request rows are removed only by the retention job';
    END IF;
END$$
CREATE TRIGGER trg_notification_request_content_no_delete BEFORE DELETE ON notification_request_content FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'notification_request_content rows are removed only by the retention job';
    END IF;
END$$
CREATE TRIGGER trg_notification_request_recipient_group_no_delete BEFORE DELETE ON notification_request_recipient_group FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'notification_request_recipient_group rows are removed only by the retention job';
    END IF;
END$$
CREATE TRIGGER trg_notification_no_delete BEFORE DELETE ON notification FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'notification rows are removed only by the retention job';
    END IF;
END$$
CREATE TRIGGER trg_notification_event_no_delete BEFORE DELETE ON notification_event FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'notification_event rows are removed only by the retention job';
    END IF;
END$$
CREATE TRIGGER trg_delivery_attempt_no_delete BEFORE DELETE ON delivery_attempt FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'delivery_attempt rows are removed only by the retention job';
    END IF;
END$$
CREATE TRIGGER trg_delivery_result_no_delete BEFORE DELETE ON delivery_result FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'delivery_result rows are removed only by the retention job';
    END IF;
END$$
DELIMITER ;

GRANT UPDATE ON notification_channel TO 'eldercare_app'@'%';
GRANT INSERT ON notification_request TO 'eldercare_app'@'%';
GRANT INSERT ON notification_request_duplicate TO 'eldercare_app'@'%';
GRANT INSERT ON notification_request_content TO 'eldercare_app'@'%';
GRANT INSERT ON notification_request_recipient_group TO 'eldercare_app'@'%';
GRANT INSERT ON notification TO 'eldercare_app'@'%';
GRANT INSERT ON notification_event TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE ON notification_state TO 'eldercare_app'@'%';
GRANT INSERT ON delivery_attempt TO 'eldercare_app'@'%';
GRANT INSERT ON delivery_result TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE, DELETE ON push_device TO 'eldercare_app'@'%';
GRANT INSERT ON m13_job_run TO 'eldercare_app'@'%';
GRANT DELETE ON notification_request TO 'eldercare_retention'@'%';
GRANT DELETE ON notification_request_duplicate TO 'eldercare_retention'@'%';
GRANT DELETE ON notification_request_content TO 'eldercare_retention'@'%';
GRANT DELETE ON notification_request_recipient_group TO 'eldercare_retention'@'%';
GRANT DELETE ON notification TO 'eldercare_retention'@'%';
GRANT DELETE ON notification_event TO 'eldercare_retention'@'%';
GRANT DELETE ON notification_state TO 'eldercare_retention'@'%';
GRANT DELETE ON delivery_attempt TO 'eldercare_retention'@'%';
GRANT DELETE ON delivery_result TO 'eldercare_retention'@'%';
