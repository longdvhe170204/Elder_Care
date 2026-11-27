-- Q-453: khai báo mốc nhắc công bố tháng theo Phụ lục 25; job nhắc vẫn gác lại.
-- Q-451: khoảng thời gian hiển thị bản kế hoạch chăm sóc đã duyệt, mặc định 7 ngày.
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M04-14', 'Ngày trong tháng nhắc Trưởng tầng công bố công việc tháng sau',
        'INTEGER', NULL, '{"min":1,"max":31}', '25', '25', 'BR-M04-01', 0),
       ('CFG-M04-15', 'Khoảng thời gian hiển thị kế hoạch chăm sóc đã duyệt',
        'DURATION', 'DAYS', '{"min":1}', '{"value":7,"unit":"DAYS"}', '{"value":7,"unit":"DAYS"}', '005 FR-059', 0);

INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, starts_at, command, actor_id, by_system,
                            reason, details, unique_key)
SELECT 'M15.PARAMETER', 0, code, 'M15', 'POINT', UTC_TIMESTAMP(6), 'CHANGE', NULL, TRUE, NULL,
       JSON_OBJECT('value', current_value),
       CONCAT('M15.PARAMETER:', code, ':', DATE_FORMAT(UTC_TIMESTAMP(6), '%Y-%m-%dT%H:%i:%s.%f'))
FROM parameter
WHERE code IN ('CFG-M04-14', 'CFG-M04-15');
