-- Gộp bảng về 79 (research R-15 của 000, chủ dự án yêu cầu 2026-11-13): bảng kỹ thuật device (002) thành dòng
-- technical_lock khóa 'M15.DEVICE:<id thiết bị>', updated_at là lần kết nối gần nhất (đối chiếu đồng bộ ngoại tuyến đã
-- khóa dòng này bằng SELECT … FOR UPDATE). Bỏ last_account_id: không nơi nào đọc. Cú pháp dùng được ở MySQL 8.0 và 8.4.
INSERT INTO technical_lock (lock_key, version, updated_at)
SELECT CONCAT('M15.DEVICE:', device_id), 0, last_connected_at
FROM device;

REVOKE INSERT, UPDATE ON device FROM 'eldercare_app'@'%';
DROP TABLE device;
