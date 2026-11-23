-- Phụ lục 25 thêm CFG-M02-17, CFG-M02-18 (2026-11-20, Q-419; 004 FR-043d): nhắc ghi nhận phụ lục đã ký, báo Quản lý viện khi
-- phụ lục chưa ký. Đặt sau V099_06 (lịch sử tham số ở status_history loại M15.PARAMETER, ParameterStore), để chạy được cả
-- trên schema mới lẫn schema đã gộp. Không dùng lại CFG-M02-14 → 16 đã bỏ theo Q-380. Cú pháp MySQL 8.0 và 8.4.

INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M02-17', 'Chu kỳ nhắc ghi nhận phụ lục đã ký', 'DURATION', 'DAYS', '{"min":1}', '{"value":1,"unit":"DAYS"}', '{"value":1,"unit":"DAYS"}', '6.6, Q-419', 0);
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M02-18', 'Báo Quản lý viện khi phụ lục chưa ký sau ngày duyệt', 'DURATION', 'DAYS', '{"min":1}', '{"value":7,"unit":"DAYS"}', '{"value":7,"unit":"DAYS"}', '6.6, Q-419', 0);

-- Giá trị đầu là một lần đổi của Bộ lập lịch (actor_id NULL), như V099_06 chép dòng parameter_history cũ
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
WHERE code IN ('CFG-M02-17', 'CFG-M02-18');
