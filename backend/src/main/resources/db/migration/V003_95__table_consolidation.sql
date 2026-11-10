-- 003 T066 (research R-15 bản 2 của 000; data-model 003 mục 0.1; chủ dự án duyệt 2026-11-09): làm như V001_95, V006_95.
-- Nhóm 3 (bed_state_history, room_isolation_history, bed_backfill_request) giữ bảng và id. Danh mục nhóm 1 (area,
-- floor, room_type kèm room_type_care_level, bed_transfer_reason, day_care_rest_area, day_care_session) giữ bảng và id
-- vì module khác lưu id (floor_area_id, contract.room_type_id, absence.session_id, vùng khoanh 007); chủ dự án chọn
-- giữ ngày 2026-11-09. Gộp: trạng thái theo người cao tuổi (resident_position → resident_module_state M03), dấu nhắc
-- (allocation_reminder → reminder_marker), hai dòng khóa (→ technical_lock), bảng con room_allowed_care_level → cột JSON
-- room.allowed_care_levels. Không bảng nào ngoài 003 trỏ id các bảng bị gộp; không đổi id ở bảng module khác.
-- Cú pháp dùng được ở MySQL 8.0 và 8.4. Thời điểm DATETIME(6) của 003 là UTC.

-- Khóa kỹ thuật: dòng có sẵn để SELECT … FOR SHARE / FOR UPDATE như hai bảng khóa cũ (RC chia sẻ, RR độc quyền).
INSERT INTO technical_lock (lock_key, version, updated_at)
VALUES ('M03.PHAN_BO', 0, UTC_TIMESTAMP(6)),
       ('M03.SUC_CHUA_BAN_TRU', 0, UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE lock_key = lock_key;

-- Mức chăm sóc cho phép của phòng vào cột JSON (mảng mã M01.CARE_LEVEL), giữ id phòng.
ALTER TABLE room ADD COLUMN allowed_care_levels JSON NULL;
UPDATE room r
SET r.allowed_care_levels = COALESCE(
        (SELECT JSON_ARRAYAGG(l.care_level) FROM room_allowed_care_level l WHERE l.room_id = r.id), JSON_ARRAY());
ALTER TABLE room MODIFY allowed_care_levels JSON NOT NULL DEFAULT (JSON_ARRAY());

-- Vị trí theo người cao tuổi của M03: {withoutBedSince, waitingHoldBedIds[]}; khóa tuần tự theo người là dòng
-- technical_lock M03.RESIDENT_POSITION:<residentId> (lệnh tạo khi khóa), không cần chép.
INSERT INTO resident_module_state (resident_id, module, data, version, updated_at)
SELECT resident_id,
       'M03',
       JSON_OBJECT(
               'withoutBedSince',
               IF(without_bed_since IS NULL, NULL, DATE_FORMAT(without_bed_since, '%Y-%m-%dT%H:%i:%s.%fZ')),
               'waitingHoldBedIds', waiting_hold_bed_ids),
       version,
       UTC_TIMESTAMP(6)
FROM resident_position;

-- Dấu nhắc: kind M03.ALLOCATION_REMINDER.<flagType>, đối tượng (target_type, target_id); mỗi lần đặt dấu là một chu kỳ c:
-- START:c lúc bắt đầu, c:n mỗi lần nhắc, END:c khi gỡ dấu. Dòng cũ là chu kỳ 1; bảng cũ chỉ giữ lần nhắc cuối nên các
-- lần nhắc 1 → n đều nhận last_reminded_at (như V001_95). details.at giữ mốc nghiệp vụ.
INSERT INTO reminder_marker (kind, entity_type, entity_id, marker, marked_at, details)
SELECT CONCAT('M03.ALLOCATION_REMINDER.', flag_type),
       target_type,
       target_id,
       'START:1',
       started_at,
       JSON_OBJECT('at', DATE_FORMAT(started_at, '%Y-%m-%dT%H:%i:%s.%fZ'))
FROM allocation_reminder;

INSERT INTO reminder_marker (kind, entity_type, entity_id, marker, marked_at, details)
WITH RECURSIVE seq (n) AS (SELECT 1
                           UNION ALL
                           SELECT n + 1
                           FROM seq
                           WHERE n < (SELECT COALESCE(MAX(reminder_count), 0) FROM allocation_reminder))
SELECT CONCAT('M03.ALLOCATION_REMINDER.', a.flag_type),
       a.target_type,
       a.target_id,
       CONCAT('1:', s.n),
       COALESCE(a.last_reminded_at, a.started_at),
       JSON_OBJECT('at', DATE_FORMAT(COALESCE(a.last_reminded_at, a.started_at), '%Y-%m-%dT%H:%i:%s.%fZ'))
FROM allocation_reminder a
         JOIN seq s ON s.n <= a.reminder_count;

-- Kiểm trước khi xóa bảng cũ; lỗi thì dừng (SIGNAL), bảng cũ còn nguyên.
DELIMITER $$
CREATE PROCEDURE m03_check_consolidation()
BEGIN
    IF (SELECT COUNT(*) FROM room WHERE allowed_care_levels IS NULL) > 0
        OR (SELECT COUNT(*) FROM room_allowed_care_level l
            JOIN room r ON r.id = l.room_id
            WHERE NOT JSON_CONTAINS(r.allowed_care_levels, JSON_QUOTE(l.care_level))) > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V003_95: mức chăm sóc của phòng chép thiếu';
    END IF;
    IF (SELECT COUNT(*) FROM resident_position)
        <> (SELECT COUNT(*) FROM resident_module_state WHERE module = 'M03') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V003_95: số dòng vị trí người cao tuổi lệch';
    END IF;
    IF (SELECT COUNT(*) + COALESCE(SUM(reminder_count), 0) FROM allocation_reminder)
        <> (SELECT COUNT(*) FROM reminder_marker WHERE kind LIKE 'M03.ALLOCATION\_REMINDER.%') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V003_95: số dấu nhắc lệch';
    END IF;
END$$
DELIMITER ;
CALL m03_check_consolidation();
DROP PROCEDURE m03_check_consolidation;

-- Gỡ quyền trước khi bỏ bảng cũ.
REVOKE INSERT, UPDATE, DELETE ON room_allowed_care_level FROM 'eldercare_app'@'%';
REVOKE INSERT, UPDATE ON resident_position FROM 'eldercare_app'@'%';
REVOKE DELETE ON resident_position FROM 'eldercare_retention'@'%';
REVOKE INSERT, UPDATE, DELETE ON allocation_reminder FROM 'eldercare_app'@'%';
REVOKE DELETE ON allocation_reminder FROM 'eldercare_retention'@'%';
REVOKE SELECT, UPDATE ON allocation_write_guard FROM 'eldercare_app'@'%';
REVOKE UPDATE ON day_care_capacity_guard FROM 'eldercare_app'@'%';

DROP TABLE room_allowed_care_level;
DROP TABLE resident_position;
DROP TABLE allocation_reminder;
DROP TABLE allocation_write_guard;
DROP TABLE day_care_capacity_guard;

-- Khai nhóm dữ liệu cho bảng giữ lại không có entity (R-15 ghi nhớ (5), audit F-04).
ALTER TABLE bed_state_history COMMENT = 'DataGroup=RECORD';
ALTER TABLE room_isolation_history COMMENT = 'DataGroup=RECORD';
ALTER TABLE bed_backfill_request COMMENT = 'DataGroup=RECORD';
ALTER TABLE room_type_care_level COMMENT = 'DataGroup=CATALOG';
