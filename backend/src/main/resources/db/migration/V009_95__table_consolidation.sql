-- 009 T043: gộp bảng theo research R-15 (bản 2) của 000, data-model 009 mục 0, 0.1. Không đổi nghiệp vụ, API, quyền.
-- Giữ 4 bảng: notification_request (nội dung, nhóm nhận vào cột JSON), notification (thêm cột dẫn xuất của
-- notification_state), call_reminder (thêm cột dẫn xuất của call_reminder_state), push_device. Kênh vào catalog_item;
-- sự kiện, lần gửi, kết quả, thông báo gộp, chuyển nhắc, kết quả gọi, kết quả liên hệ vào record_event; liên hệ trực
-- tiếp, khung gộp tin vào simple_record; bản trùng, lần chạy job vào reminder_marker. Dòng chép sang bảng chung cấp id
-- mới, ghi legacy = {table, id}; id tham chiếu chéo (khung gộp, lần gửi tổng hợp, liên hệ trực tiếp) đổi theo bảng ánh
-- xạ tạm. Chạy bằng tài khoản Flyway (xóa, tạo lại trigger). Cú pháp có ở MySQL 8.0, 8.4.

-- 1. Yêu cầu thông báo: phần nội dung, nhóm người nhận thành cột JSON (thứ tự theo seq trong phần tử).
ALTER TABLE notification_request
    ADD COLUMN contents         JSON NULL COMMENT 'Mảng {seq, informationType, content} (FR-013)',
    ADD COLUMN recipient_groups JSON NULL COMMENT 'Mảng {seq, groupCode, params, substitution, substituteGroup, outcome}';

DROP TRIGGER trg_notification_request_no_update;

UPDATE notification_request r
SET r.contents         = COALESCE((SELECT JSON_ARRAYAGG(JSON_OBJECT('seq', c.seq, 'informationType', c.information_type,
                                                                   'content', c.content))
                                   FROM notification_request_content c
                                   WHERE c.request_id = r.id), JSON_ARRAY()),
    r.recipient_groups = COALESCE((SELECT JSON_ARRAYAGG(JSON_OBJECT('seq', g.seq, 'groupCode', g.group_code,
                                                                   'params', g.params, 'substitution', g.substitution,
                                                                   'substituteGroup', g.substitute_group,
                                                                   'outcome', g.outcome))
                                   FROM notification_request_recipient_group g
                                   WHERE g.request_id = r.id), JSON_ARRAY());

ALTER TABLE notification_request
    MODIFY contents JSON NOT NULL COMMENT 'Mảng {seq, informationType, content} (FR-013)',
    MODIFY recipient_groups JSON NOT NULL COMMENT 'Mảng {seq, groupCode, params, substitution, substituteGroup, outcome}',
    COMMENT = 'DataGroup=RECORD';

CREATE TRIGGER trg_notification_request_no_update BEFORE UPDATE ON notification_request FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'notification_request is append-only';

-- 2. Thông báo: nhóm 3 khóa theo dòng; cột trạng thái dẫn xuất (research B-09) sửa được, mọi cột khác không.
ALTER TABLE notification
    ADD COLUMN status                 ENUM ('HELD_FOR_QUIET_HOURS', 'NOT_SENT', 'SENT', 'DELIVERY_FAILED', 'VIEWED',
        'ACKNOWLEDGED', 'UNREACHABLE') NULL COMMENT 'NULL: chưa có lần gửi nào xong',
    ADD COLUMN scheduled_send_at      DATETIME(6) NULL,
    ADD COLUMN sent_at                DATETIME(6) NULL,
    ADD COLUMN viewed_at              DATETIME(6) NULL,
    ADD COLUMN acknowledged_at        DATETIME(6) NULL,
    ADD COLUMN acknowledgement_method ENUM ('IN_APP', 'BY_CALL', 'LATE', 'LATE_BY_CALL') NULL,
    ADD COLUMN hidden                 BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN resident_id            BIGINT      NULL COMMENT 'Người cao tuổi của yêu cầu',
    ADD COLUMN recipient_key          VARCHAR(40) GENERATED ALWAYS AS (CONCAT(recipient_type, ':', recipient_id)) STORED,
    ADD KEY idx_notification_inbox (recipient_key, sent_at),
    ADD KEY idx_notification_due (status, scheduled_send_at),
    ADD KEY idx_notification_level (level, status, sent_at),
    COMMENT = 'DataGroup=MULTIPLE';

DROP TRIGGER trg_notification_no_update;

UPDATE notification n
    JOIN notification_state s ON s.notification_id = n.id
SET n.status                 = s.status,
    n.scheduled_send_at      = s.scheduled_send_at,
    n.sent_at                = s.sent_at,
    n.viewed_at              = s.viewed_at,
    n.acknowledged_at        = s.acknowledged_at,
    n.acknowledgement_method = s.acknowledgement_method,
    n.hidden                 = s.hidden,
    n.resident_id            = s.resident_id;

DELIMITER $$
CREATE TRIGGER trg_notification_row_lock BEFORE UPDATE ON notification FOR EACH ROW
BEGIN
    IF NOT (OLD.id <=> NEW.id) OR NOT (OLD.request_id <=> NEW.request_id)
        OR NOT (OLD.recipient_type <=> NEW.recipient_type) OR NOT (OLD.recipient_id <=> NEW.recipient_id)
        OR NOT (OLD.account_id <=> NEW.account_id) OR NOT (OLD.basis <=> NEW.basis)
        OR NOT (OLD.visible_parts <=> NEW.visible_parts) OR NOT (OLD.identity_only <=> NEW.identity_only)
        OR NOT (OLD.q354_case <=> NEW.q354_case) OR NOT (OLD.level <=> NEW.level)
        OR NOT (OLD.created_at <=> NEW.created_at) OR NOT (OLD.resident_id <=> NEW.resident_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'notification is row-locked: only derived status columns change';
    END IF;
END$$
DELIMITER ;

GRANT UPDATE (status, scheduled_send_at, sent_at, viewed_at, acknowledged_at, acknowledgement_method, hidden)
    ON notification TO 'eldercare_app'@'%';

-- 3. Lượt nhắc gọi: nhóm 3 khóa theo dòng; cột trạng thái dẫn xuất (FR-030) sửa được; một lượt Chờ kết quả mỗi người cần
-- gọi (pending_key, FR-032).
ALTER TABLE call_reminder
    ADD COLUMN status           ENUM ('AWAITING_RESULT', 'RESULT_RECORDED', 'STOPPED') NULL,
    ADD COLUMN final_result     ENUM ('REACHED', 'NOT_REACHED') NULL,
    ADD COLUMN stop_reason      VARCHAR(100) NULL,
    ADD COLUMN due_at           DATETIME(6)  NULL,
    ADD COLUMN current_staff_id BIGINT       NULL COMMENT 'Người được nhắc hiện tại',
    ADD COLUMN callee_key       VARCHAR(191) NULL COMMENT 'loại:id người cần gọi:resident:id hoặc source:loại:id';

DROP TRIGGER trg_call_reminder_no_update;

UPDATE call_reminder r
    JOIN call_reminder_state s ON s.call_reminder_id = r.id
SET r.status           = s.status,
    r.final_result     = s.final_result,
    r.stop_reason      = s.stop_reason,
    r.due_at           = s.due_at,
    r.current_staff_id = s.current_staff_id,
    r.callee_key       = s.callee_key;

ALTER TABLE call_reminder
    MODIFY status ENUM ('AWAITING_RESULT', 'RESULT_RECORDED', 'STOPPED') NOT NULL,
    MODIFY due_at DATETIME(6) NOT NULL,
    MODIFY current_staff_id BIGINT NOT NULL COMMENT 'Người được nhắc hiện tại',
    MODIFY callee_key VARCHAR(191) NOT NULL COMMENT 'loại:id người cần gọi:resident:id hoặc source:loại:id',
    ADD COLUMN pending_key VARCHAR(191) GENERATED ALWAYS AS (IF(status = 'AWAITING_RESULT', callee_key, NULL)) STORED,
    ADD CONSTRAINT uq_call_reminder_pending UNIQUE (pending_key),
    ADD KEY idx_call_reminder_due (status, due_at),
    COMMENT = 'DataGroup=MULTIPLE';

DELIMITER $$
CREATE TRIGGER trg_call_reminder_row_lock BEFORE UPDATE ON call_reminder FOR EACH ROW
BEGIN
    IF NOT (OLD.id <=> NEW.id) OR NOT (OLD.call_chain_id <=> NEW.call_chain_id)
        OR NOT (OLD.callee_type <=> NEW.callee_type) OR NOT (OLD.callee_id <=> NEW.callee_id)
        OR NOT (OLD.resident_id <=> NEW.resident_id) OR NOT (OLD.source_record_type <=> NEW.source_record_type)
        OR NOT (OLD.source_record_id <=> NEW.source_record_id) OR NOT (OLD.floor_area_id <=> NEW.floor_area_id)
        OR NOT (OLD.creation_reason <=> NEW.creation_reason) OR NOT (OLD.created_at <=> NEW.created_at)
        OR NOT (OLD.callee_key <=> NEW.callee_key) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'call_reminder is row-locked: only derived status columns change';
    END IF;
END$$
DELIMITER ;

GRANT UPDATE (status, final_result, stop_reason, due_at, current_staff_id) ON call_reminder TO 'eldercare_app'@'%';

ALTER TABLE push_device
    COMMENT = 'DataGroup=TECHNICAL';

-- 4. Kênh thông báo vào danh mục chung.
INSERT INTO catalog_item (catalog_type, module, code, name, sort_order, status, attributes, version)
SELECT 'M13.NOTIFICATION_CHANNEL', 'M13', code, name, 0, 'ACTIVE',
       JSON_OBJECT('enabled', IF(enabled, CAST('true' AS JSON), CAST('false' AS JSON)),
                   'levels', IF(levels = '', JSON_ARRAY(), CAST(CONCAT('["', REPLACE(levels, ',', '","'), '"]') AS JSON)),
                   'config', config),
       version
FROM notification_channel
ORDER BY code;

-- 5. Lần gửi: thêm dạng nháp (khung gộp chưa có id mới), xác nhận sau khi đổi batchWindowId.
INSERT INTO record_event (entity_type, entity_id, module, kind, content, by_system, occurred_at, unique_key)
SELECT 'M13.NOTIFICATION', a.notification_id, 'M13', 'M13.DELIVERY_ATTEMPT',
       JSON_OBJECT('channel', a.channel, 'address', a.address, 'attemptNo', a.attempt_no, 'batchWindowId', NULL,
                   'content', a.content, 'legacy', JSON_OBJECT('table', 'delivery_attempt', 'id', a.id)),
       TRUE, a.scheduled_at, CONCAT('M13.DA:', a.notification_id, ':', a.channel, ':', a.attempt_no)
FROM delivery_attempt a
ORDER BY a.id;

CREATE TABLE m13_v95_attempt
(
    old_id BIGINT NOT NULL PRIMARY KEY,
    new_id BIGINT NOT NULL
);
INSERT INTO m13_v95_attempt (old_id, new_id)
SELECT JSON_VALUE(content, '$.legacy.id' RETURNING SIGNED), id
FROM record_event
WHERE kind = 'M13.DELIVERY_ATTEMPT' AND JSON_VALUE(content, '$.legacy.table') = 'delivery_attempt';

-- 6. Khung gộp tin nhắn vào simple_record: một khung mở mỗi người nhận bằng unique_key.
INSERT INTO simple_record (kind, module, status, content, unique_key, created_at, created_by)
SELECT 'M13.SMS_BATCH_WINDOW', 'M13', IF(w.closed_at IS NULL, 'OPEN', 'CLOSED'),
       JSON_OBJECT('recipientKey', w.recipient_key,
                   'startedAt', DATE_FORMAT(w.started_at, '%Y-%m-%dT%H:%i:%s.%fZ'),
                   'endsAt', DATE_FORMAT(w.ends_at, '%Y-%m-%dT%H:%i:%s.%fZ'),
                   'firstAttemptId', f.new_id,
                   'closedAt', DATE_FORMAT(w.closed_at, '%Y-%m-%dT%H:%i:%s.%fZ'),
                   'digestAttemptId', d.new_id,
                   'legacy', JSON_OBJECT('table', 'sms_batch_window', 'id', w.id)),
       IF(w.closed_at IS NULL, CONCAT('M13.SBW:', w.recipient_key), NULL), w.started_at, 0
FROM sms_batch_window w
         JOIN m13_v95_attempt f ON f.old_id = w.first_attempt_id
         LEFT JOIN m13_v95_attempt d ON d.old_id = w.digest_attempt_id
ORDER BY w.id;

CREATE TABLE m13_v95_window
(
    old_id BIGINT NOT NULL PRIMARY KEY,
    new_id BIGINT NOT NULL
);
INSERT INTO m13_v95_window (old_id, new_id)
SELECT JSON_VALUE(content, '$.legacy.id' RETURNING SIGNED), id
FROM simple_record
WHERE kind = 'M13.SMS_BATCH_WINDOW';

UPDATE record_event e
    JOIN m13_v95_attempt m ON m.new_id = e.id
    JOIN delivery_attempt a ON a.id = m.old_id
    LEFT JOIN m13_v95_window w ON w.old_id = a.batch_window_id
SET e.content      = JSON_SET(e.content, '$.batchWindowId', w.new_id),
    e.confirmed_at = e.occurred_at;

-- 7. Kết quả lần gửi: một kết quả mỗi lần gửi (unique_key).
INSERT INTO record_event (entity_type, entity_id, module, kind, content, by_system, occurred_at, confirmed_at,
                          unique_key)
SELECT 'M13.DELIVERY_ATTEMPT', m.new_id, 'M13', 'M13.DELIVERY_RESULT',
       JSON_OBJECT('result', r.result, 'reason', r.reason, 'digestAttemptId', d.new_id,
                   'legacy', JSON_OBJECT('table', 'delivery_result', 'id', r.id)),
       TRUE, r.occurred_at, r.occurred_at, CONCAT('M13.DR:', m.new_id)
FROM delivery_result r
         JOIN m13_v95_attempt m ON m.old_id = r.attempt_id
         LEFT JOIN m13_v95_attempt d ON d.old_id = r.digest_attempt_id
ORDER BY r.id;

-- 8. Sự kiện của thông báo: gửi, không gửi, xem, xác nhận mỗi loại một lần (unique_key thay once_key).
INSERT INTO record_event (entity_type, entity_id, module, kind, content, actor_id, by_system, occurred_at,
                          confirmed_at, unique_key)
SELECT 'M13.NOTIFICATION', e.notification_id, 'M13', 'M13.NOTIFICATION_EVENT',
       JSON_OBJECT('type', e.type, 'acknowledgementMethod', e.acknowledgement_method, 'reason', e.reason,
                   'scheduledSendAt', DATE_FORMAT(e.scheduled_send_at, '%Y-%m-%dT%H:%i:%s.%fZ'),
                   'legacy', JSON_OBJECT('table', 'notification_event', 'id', e.id)),
       e.actor_id, e.actor_id IS NULL, e.occurred_at, e.occurred_at,
       IF(e.type IN ('SEND', 'NOT_SENT', 'VIEW', 'ACKNOWLEDGE'), CONCAT('M13.NE:', e.notification_id, ':', e.type), NULL)
FROM notification_event e
ORDER BY e.id;

-- 9. Cần liên hệ trực tiếp vào simple_record (khóa khi rời Chờ liên hệ), kết quả liên hệ vào record_event.
INSERT INTO simple_record (kind, module, entity_type, entity_id, status, content, unique_key, locked_at, created_at,
                           created_by, version)
SELECT 'M13.DIRECT_CONTACT', 'M13', 'M13.NOTIFICATION', d.notification_id, d.status,
       JSON_OBJECT('familyMemberId', d.family_member_id, 'fullName', d.full_name, 'relationship', d.relationship,
                   'phoneNumber', d.phone_number, 'shortContent', d.short_content,
                   'remindedAt', DATE_FORMAT(d.reminded_at, '%Y-%m-%dT%H:%i:%s.%fZ'),
                   'legacy', JSON_OBJECT('table', 'direct_contact', 'id', d.id)),
       CONCAT('M13.DC:', d.notification_id),
       IF(d.status = 'PENDING', NULL,
          COALESCE((SELECT x.recorded_at FROM direct_contact_result x WHERE x.direct_contact_id = d.id), d.created_at)),
       d.created_at, 0, d.version
FROM direct_contact d
ORDER BY d.id;

CREATE TABLE m13_v95_contact
(
    old_id BIGINT NOT NULL PRIMARY KEY,
    new_id BIGINT NOT NULL
);
INSERT INTO m13_v95_contact (old_id, new_id)
SELECT JSON_VALUE(content, '$.legacy.id' RETURNING SIGNED), id
FROM simple_record
WHERE kind = 'M13.DIRECT_CONTACT';

INSERT INTO record_event (entity_type, entity_id, module, kind, content, actor_id, by_system, occurred_at,
                          confirmed_at, unique_key)
SELECT 'M13.DIRECT_CONTACT', m.new_id, 'M13', 'M13.DIRECT_CONTACT_RESULT',
       JSON_OBJECT('occurredAt', DATE_FORMAT(r.occurred_at, '%Y-%m-%dT%H:%i:%s.%fZ'), 'result', r.result,
                   'reason', r.reason, 'note', r.note,
                   'legacy', JSON_OBJECT('table', 'direct_contact_result', 'id', r.id)),
       r.contacted_by, FALSE, r.recorded_at, r.recorded_at, CONCAT('M13.DCR:', m.new_id)
FROM direct_contact_result r
         JOIN m13_v95_contact m ON m.old_id = r.direct_contact_id
ORDER BY r.id;

-- 10. Thông báo gộp vào lượt (một lượt mỗi thông báo), chuyển nhắc, kết quả gọi.
INSERT INTO record_event (entity_type, entity_id, module, kind, content, by_system, occurred_at, confirmed_at,
                          unique_key)
SELECT 'M13.CALL_REMINDER', l.call_reminder_id, 'M13', 'M13.CALL_REMINDER_NOTIFICATION',
       JSON_OBJECT('notificationId', l.notification_id,
                   'legacy', JSON_OBJECT('table', 'call_reminder_notification', 'id', l.id)),
       TRUE, l.merged_at, l.merged_at, CONCAT('M13.CRN:', l.notification_id)
FROM call_reminder_notification l
ORDER BY l.id;

INSERT INTO record_event (entity_type, entity_id, module, kind, content, by_system, occurred_at, confirmed_at)
SELECT 'M13.CALL_REMINDER', h.call_reminder_id, 'M13', 'M13.CALL_REMINDER_HANDOFF',
       JSON_OBJECT('previousStaffId', h.previous_staff_id, 'remindedStaffId', h.reminded_staff_id, 'tier', h.tier,
                   'reason', h.reason, 'dueAt', DATE_FORMAT(h.due_at, '%Y-%m-%dT%H:%i:%s.%fZ'),
                   'legacy', JSON_OBJECT('table', 'call_reminder_handoff', 'id', h.id)),
       TRUE, h.occurred_at, h.occurred_at
FROM call_reminder_handoff h
ORDER BY h.id;

INSERT INTO record_event (entity_type, entity_id, module, kind, content, actor_id, by_system, occurred_at,
                          confirmed_at)
SELECT 'M13.CALL_REMINDER', c.call_reminder_id, 'M13', 'M13.CALL_RESULT',
       JSON_OBJECT('calledAt', DATE_FORMAT(c.called_at, '%Y-%m-%dT%H:%i:%s.%fZ'),
                   'calledPhoneNumber', c.called_phone_number, 'result', c.result, 'reason', c.reason,
                   'answeredBy', c.answered_by, 'note', c.note,
                   'supplementary', IF(c.supplementary, CAST('true' AS JSON), CAST('false' AS JSON)),
                   'legacy', JSON_OBJECT('table', 'call_result', 'id', c.id)),
       c.called_by, FALSE, c.recorded_at, c.recorded_at
FROM call_result c
ORDER BY c.id;

-- 11. Bản trùng, ngày đã chạy xong của job hằng ngày vào reminder_marker.
INSERT IGNORE INTO reminder_marker (kind, entity_type, entity_id, marker, marked_at)
SELECT 'M13.REQUEST_DUPLICATE', 'M13.NOTIFICATION_REQUEST', request_id,
       DATE_FORMAT(received_at, '%Y-%m-%dT%H:%i:%s.%fZ'), received_at
FROM notification_request_duplicate;

INSERT INTO reminder_marker (kind, entity_type, entity_id, marker, marked_at)
SELECT 'M13.JOB_DONE', job, 0, DATE_FORMAT(business_date, '%Y-%m-%d'), completed_at
FROM m13_job_run;

-- 12. Bỏ bảng cũ (gỡ quyền trước: quyền theo bảng không tự mất khi DROP TABLE).
REVOKE UPDATE ON notification_channel FROM 'eldercare_app'@'%';
REVOKE INSERT ON notification_request_duplicate FROM 'eldercare_app'@'%';
REVOKE INSERT ON notification_request_content FROM 'eldercare_app'@'%';
REVOKE INSERT ON notification_request_recipient_group FROM 'eldercare_app'@'%';
REVOKE INSERT ON notification_event FROM 'eldercare_app'@'%';
REVOKE INSERT, UPDATE ON notification_state FROM 'eldercare_app'@'%';
REVOKE INSERT ON delivery_attempt FROM 'eldercare_app'@'%';
REVOKE INSERT ON delivery_result FROM 'eldercare_app'@'%';
REVOKE INSERT ON m13_job_run FROM 'eldercare_app'@'%';
REVOKE INSERT, UPDATE ON sms_batch_window FROM 'eldercare_app'@'%';
REVOKE INSERT, UPDATE ON direct_contact FROM 'eldercare_app'@'%';
REVOKE INSERT ON direct_contact_result FROM 'eldercare_app'@'%';
REVOKE INSERT ON call_reminder_notification FROM 'eldercare_app'@'%';
REVOKE INSERT ON call_reminder_handoff FROM 'eldercare_app'@'%';
REVOKE INSERT, UPDATE ON call_reminder_state FROM 'eldercare_app'@'%';
REVOKE INSERT ON call_result FROM 'eldercare_app'@'%';
REVOKE DELETE ON notification_request_duplicate FROM 'eldercare_retention'@'%';
REVOKE DELETE ON notification_request_content FROM 'eldercare_retention'@'%';
REVOKE DELETE ON notification_request_recipient_group FROM 'eldercare_retention'@'%';
REVOKE DELETE ON notification_event FROM 'eldercare_retention'@'%';
REVOKE DELETE ON notification_state FROM 'eldercare_retention'@'%';
REVOKE DELETE ON delivery_attempt FROM 'eldercare_retention'@'%';
REVOKE DELETE ON delivery_result FROM 'eldercare_retention'@'%';
REVOKE DELETE ON sms_batch_window FROM 'eldercare_retention'@'%';
REVOKE DELETE ON direct_contact FROM 'eldercare_retention'@'%';
REVOKE DELETE ON direct_contact_result FROM 'eldercare_retention'@'%';
REVOKE DELETE ON call_reminder_notification FROM 'eldercare_retention'@'%';
REVOKE DELETE ON call_reminder_handoff FROM 'eldercare_retention'@'%';
REVOKE DELETE ON call_reminder_state FROM 'eldercare_retention'@'%';
REVOKE DELETE ON call_result FROM 'eldercare_retention'@'%';

DROP TABLE direct_contact_result;
DROP TABLE direct_contact;
DROP TABLE delivery_result;
DROP TABLE delivery_attempt;
DROP TABLE notification_event;
DROP TABLE notification_state;
DROP TABLE call_reminder_notification;
DROP TABLE call_reminder_handoff;
DROP TABLE call_reminder_state;
DROP TABLE call_result;
DROP TABLE notification_request_content;
DROP TABLE notification_request_recipient_group;
DROP TABLE notification_request_duplicate;
DROP TABLE sms_batch_window;
DROP TABLE m13_job_run;
DROP TABLE notification_channel;
DROP TABLE m13_v95_attempt;
DROP TABLE m13_v95_window;
DROP TABLE m13_v95_contact;
