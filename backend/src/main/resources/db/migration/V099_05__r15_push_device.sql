-- Gộp bảng về 79 (research R-15 của 000, chủ dự án yêu cầu 2026-11-13): thiết bị nhận đẩy (009) thành dòng simple_record
-- loại M13.PUSH_DEVICE của tài khoản; unique_key 'M13.PUSH_DEVICE:<tài khoản>:<thiết bị>' thay khóa chính cũ; trạng thái
-- ACTIVE, INVALIDATED (FCM báo token hết hạn); app, token ở content. 009 vẫn tự quản lý token, không mở API sang 002.
-- Cú pháp dùng được ở MySQL 8.0 và 8.4.
INSERT INTO simple_record (kind, module, entity_type, entity_id, status, content, unique_key, created_at, created_by)
SELECT 'M13.PUSH_DEVICE',
       'M13',
       'M15.ACCOUNT',
       account_id,
       IF(invalidated_at IS NULL, 'ACTIVE', 'INVALIDATED'),
       JSON_OBJECT('deviceId', device_id,
                   'app', CONCAT(app, ''),
                   'fcmToken', fcm_token,
                   'updatedAt', DATE_FORMAT(updated_at, '%Y-%m-%dT%H:%i:%s.%fZ'),
                   'invalidatedAt',
                   IF(invalidated_at IS NULL, NULL, DATE_FORMAT(invalidated_at, '%Y-%m-%dT%H:%i:%s.%fZ'))),
       CONCAT('M13.PUSH_DEVICE:', account_id, ':', device_id),
       updated_at,
       account_id
FROM push_device;

REVOKE INSERT, UPDATE, DELETE ON push_device FROM 'eldercare_app'@'%';
DROP TABLE push_device;
