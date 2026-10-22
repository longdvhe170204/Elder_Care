-- Phụ lục 25 thêm ba tham số (2026-10-22): CFG-M05-09 (Q-373), CFG-M09-13 (Q-382), CFG-M11-11 (Q-376, Q-377, Q-383).
-- Cách nạp như V000_04: một dòng parameter và một dòng parameter_history (Bộ lập lịch, actor_id NULL).
-- Cú pháp dùng được ở MySQL 8.0 và 8.4 (research R-01 của 000).

-- CFG-M05-09: 30 giây
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M05-09', 'Chu kỳ ứng dụng gửi lại vị trí khi cảnh báo từ nút SOS, nút hỗ trợ khẩn cấp chưa có người tới nơi', 'DURATION', 'SECONDS', '{"min":1}', '{"value":30,"unit":"SECONDS"}', '{"value":30,"unit":"SECONDS"}', 'BR-M05-28', 0);
-- CFG-M09-13: 500 dòng
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M09-13', 'Số dòng tối đa của một file import danh sách nhân viên', 'INTEGER', 'ROWS', '{"min":1}', '500', '500', 'BR-M09-18', 0);
-- CFG-M11-11: Viện nhập khi cài đặt
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M11-11', 'Thông tin viện in trên chứng từ, phiếu tóm tắt chuyển viện và hiện ở màn gọi 115 (tên, địa chỉ, mã số thuế, điện thoại, logo)', 'TABLE', NULL, NULL, 'null', 'null', '15.9a, BR-M05-31, BR-M05-32', 0);

INSERT INTO parameter_history (code, value, effective_from, actor_id, reason, is_default_restore)
SELECT code, current_value, UTC_TIMESTAMP(6), NULL, NULL, FALSE FROM parameter
WHERE code IN ('CFG-M05-09', 'CFG-M09-13', 'CFG-M11-11');
