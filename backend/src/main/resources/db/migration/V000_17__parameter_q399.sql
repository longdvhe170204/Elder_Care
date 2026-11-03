-- Phụ lục 25 thêm ba tham số (2026-11-03, Q-399): tự đặt lại mật khẩu qua tin nhắn (19.1, BR-M15-25, 002 FR-052a).
-- Cách nạp như V000_08: một dòng parameter và một dòng parameter_history (Bộ lập lịch, actor_id NULL).
-- Số V000_17 thấp hơn migration đã chạy trên schema dev: profile demo đã bật out-of-order; profile khác xem team-plan
-- mục 8 dòng "Ghi nhớ migration số nhỏ đến sau". Cú pháp dùng được ở MySQL 8.0 và 8.4 (research R-01 của 000).

-- CFG-M15-12: 5 phút
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M15-12', 'Hiệu lực mã đặt lại mật khẩu gửi qua tin nhắn', 'DURATION', 'MINUTES', '{"min":1}', '{"value":5,"unit":"MINUTES"}', '{"value":5,"unit":"MINUTES"}', '19.1, BR-M15-25', 0);
-- CFG-M15-13: 5 lần
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M15-13', 'Số lần nhập sai mã đặt lại mật khẩu trước khi mã bị hủy', 'INTEGER', NULL, '{"min":1}', '5', '5', 'BR-M15-25', 0);
-- CFG-M15-14: tối đa 5 lần mỗi ngày, cách nhau ít nhất 60 giây
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M15-14', 'Giới hạn xin mã đặt lại mật khẩu của một tài khoản', 'TABLE', NULL, NULL, '{"maxPerDay":5,"minInterval":{"value":60,"unit":"SECONDS"}}', '{"maxPerDay":5,"minInterval":{"value":60,"unit":"SECONDS"}}', 'BR-M15-25', 0);

INSERT INTO parameter_history (code, value, effective_from, actor_id, reason, is_default_restore)
SELECT code, current_value, UTC_TIMESTAMP(6), NULL, NULL, FALSE FROM parameter
WHERE code IN ('CFG-M15-12', 'CFG-M15-13', 'CFG-M15-14');
