-- Phụ lục 25 thêm CFG-M07-08 → 11 và CFG-M11-12 (2026-11-22, Q-427 → Q-437, sửa BF-07): thời hạn nhận liều, mốc Bỏ lỡ của đơn
-- Quan trọng về thời gian, hạn leo thang đối chiếu thuốc, nhắc ghi hiệu quả PRN, nhắc yêu cầu mua thuốc Gấp chờ duyệt.
-- Đặt sau V099_09, cùng cách ghi lịch sử tham số. Cú pháp MySQL 8.0 và 8.4.

INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M07-08', 'Thời hạn nhận liều tự hết nếu chưa xác nhận', 'DURATION', 'MINUTES', '{"min":1}', '{"value":15,"unit":"MINUTES"}', '{"value":15,"unit":"MINUTES"}', 'BR-M07-26', 0);
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M07-09', 'Thời gian từ Trễ chuyển sang Bỏ lỡ của đơn Quan trọng về thời gian', 'DURATION', 'MINUTES', '{"min":1}', '{"value":15,"unit":"MINUTES"}', '{"value":15,"unit":"MINUTES"}', 'BR-M07-27', 0);
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M07-10', 'Hạn đối chiếu thuốc để báo thêm Trưởng tầng và Quản lý viện', 'DURATION', 'HOURS', '{"min":1}', '{"value":8,"unit":"HOURS"}', '{"value":8,"unit":"HOURS"}', 'BR-M07-29', 0);
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M07-11', 'Nhắc ghi hiệu quả sau lần dùng thuốc khi cần (PRN)', 'DURATION', 'MINUTES', '{"min":1}', '{"value":60,"unit":"MINUTES"}', '{"value":60,"unit":"MINUTES"}', 'BR-M07-04', 0);
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M11-12', 'Thời gian yêu cầu mua thuốc mức Gấp chờ Quản lý viện duyệt thì báo Quản lý viện', 'DURATION', 'HOURS', '{"min":1}', '{"value":24,"unit":"HOURS"}', '{"value":24,"unit":"HOURS"}', 'BR-M11-22', 0);

INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, starts_at, command, actor_id, by_system,
                            reason, details, unique_key)
SELECT 'M15.PARAMETER',
       0,
       code,
       'M15',
       'POINT',
       UTC_TIMESTAMP(6),
       'CHANGE',
       NULL,
       TRUE,
       NULL,
       JSON_OBJECT('value', current_value),
       CONCAT('M15.PARAMETER:', code, ':', DATE_FORMAT(UTC_TIMESTAMP(6), '%Y-%m-%dT%H:%i:%s.%f'))
FROM parameter
WHERE code IN ('CFG-M07-08', 'CFG-M07-09', 'CFG-M07-10', 'CFG-M07-11', 'CFG-M11-12');
