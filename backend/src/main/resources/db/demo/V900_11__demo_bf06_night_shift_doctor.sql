-- Dữ liệu demo BF-06 (chỉ profile demo; 2026-11-24, Đức Anh): bản demo hay chạy đồng hồ giả lập sang buổi tối để thử
-- leo thang, nhưng V900_02, V900_09 chỉ có ca ngày 07:00–18:00. Ngoài giờ đó không ai trong ca nên Điều dưỡng không thấy
-- cảnh báo, chuyển viện không chọn được nhân viên bàn giao, và mọi sự cố ghi "Không có bác sĩ trực". Thêm:
--   1. Mẫu ca đêm 9002, 18:00–07:00 giờ Việt Nam (11:00–24:00 UTC), lịch ca 7 ngày kể từ ngày nạp ở Tầng 1 (người phụ
--      trách ca 9005, thành viên 9005, 9006) và Tầng 2 (9011, 9012), như ca ngày;
--   2. Bác sĩ trực: demo.bacsi (9004) là thành viên ca ngày và ca đêm Tầng 1 với on_duty_doctor, để cảnh báo, sự cố báo
--      đúng Bác sĩ trực (BR-M05-06) và leo thang cấp 1 có Bác sĩ trực (BR-M05-01);
--   3. Phân công Điều dưỡng 9005, Nhân viên chăm sóc 9006 cho người cao tuổi 9001 trong ca đêm Tầng 1, như ca ngày.
-- Bảng và cách ghi như V900_02, V900_03, V900_09. Cú pháp dùng được ở MySQL 8.0 và 8.4.

SET @demo_at = UTC_TIMESTAMP(6);
SET @today = DATE(@demo_at + INTERVAL 7 HOUR);
SET @manager = (SELECT id FROM account WHERE username = 'demo.quanly');

-- 008: mẫu ca đêm (createdAt dạng ISO-8601 UTC như V900_03)
INSERT INTO catalog_item (id, catalog_type, module, name, status, attributes)
VALUES (9002, 'M09.SHIFT_TEMPLATE', 'M09', 'Ca đêm', 'ACTIVE',
        JSON_OBJECT('createdAt', DATE_FORMAT(@demo_at, '%Y-%m-%dT%H:%i:%s.%fZ'), 'startTime', '18:00:00',
                    'endTime', '07:00:00', 'scopeType', 'FLOOR', 'requiresHandover', CAST('false' AS JSON),
                    'originalName', 'Ca đêm'));

CREATE TEMPORARY TABLE demo_day (k INT PRIMARY KEY);
INSERT INTO demo_day (k) VALUES (0), (1), (2), (3), (4), (5), (6);

CREATE TEMPORARY TABLE demo_floor (floor_id BIGINT PRIMARY KEY, lead_id BIGINT, member_id BIGINT);
INSERT INTO demo_floor (floor_id, lead_id, member_id) VALUES (9001, 9005, 9006), (9002, 9011, 9012);

INSERT IGNORE INTO shift_schedule (month, scope_type, scope_id, status)
SELECT DISTINCT DATE_FORMAT(@today + INTERVAL d.k DAY, '%Y-%m'), 'FLOOR', f.floor_id, 'PUBLISHED'
FROM demo_day d JOIN demo_floor f;

INSERT INTO work_shift (schedule_id, template_id, scope_type, scope_id, starts_at, ends_at, requires_handover, status,
                        lead_staff_id)
SELECT s.id, 9002, 'FLOOR', f.floor_id,
       TIMESTAMP(@today + INTERVAL d.k DAY) + INTERVAL 11 HOUR,
       TIMESTAMP(@today + INTERVAL d.k DAY) + INTERVAL 24 HOUR,
       FALSE, 'PUBLISHED', f.lead_id
FROM demo_day d
         JOIN demo_floor f
         JOIN shift_schedule s ON s.month = DATE_FORMAT(@today + INTERVAL d.k DAY, '%Y-%m') AND s.scope_type = 'FLOOR'
    AND s.scope_id = f.floor_id
ORDER BY f.floor_id, d.k;

CREATE TEMPORARY TABLE demo_night AS
SELECT w.id, w.scope_id, w.starts_at, w.ends_at, f.lead_id, f.member_id
FROM work_shift w JOIN demo_floor f ON f.floor_id = w.scope_id
WHERE w.template_id = 9002 AND w.scope_type = 'FLOOR';

-- Bảng tạm của MySQL không đọc hai lần trong một câu lệnh: thêm người phụ trách và thành viên bằng hai lệnh
INSERT INTO shift_staff (shift_id, staff_id)
SELECT n.id, n.lead_id FROM demo_night n;

INSERT INTO shift_staff (shift_id, staff_id)
SELECT n.id, n.member_id FROM demo_night n;

INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value_id, starts_at, command, by_system)
SELECT 'M09.SHIFT', n.id, 'LEAD_STAFF', 'M09', 'PERIOD', n.lead_id, n.starts_at, 'SCHEDULED', FALSE FROM demo_night n;

-- Bác sĩ trực: ca đêm Tầng 1 và các ca ngày Tầng 1 chưa đóng, chưa hủy (ca đã đóng không sửa được, trigger FR-022)
INSERT INTO shift_staff (shift_id, staff_id, on_duty_doctor)
SELECT n.id, 9004, TRUE FROM demo_night n WHERE n.scope_id = 9001;

INSERT INTO shift_staff (shift_id, staff_id, on_duty_doctor)
SELECT w.id, 9004, TRUE
FROM work_shift w
WHERE w.template_id = 9001
  AND w.scope_type = 'FLOOR'
  AND w.scope_id = 9001
  AND w.status NOT IN ('CLOSED', 'CANCELLED')
  AND w.ends_at > @demo_at - INTERVAL 1 DAY
  AND NOT EXISTS (SELECT 1 FROM shift_staff x WHERE x.shift_id = w.id AND x.staff_id = 9004);

-- Phân công trong ca đêm Tầng 1 cho người cao tuổi 9001, như V900_02
INSERT INTO staff_assignment (shift_id, staff_id, target_type, target_id, role, assignment_kind, assigned_nurse,
                              starts_at, ends_at, status, created_by)
SELECT n.id, p.staff_id, 'RESIDENT', 9001, p.role, 'PRIMARY', p.nurse, n.starts_at, n.ends_at,
       IF(n.starts_at <= @demo_at, 'ACTIVE', 'SCHEDULED'), @manager
FROM demo_night n
         JOIN (SELECT 9006 AS staff_id, 'CAREGIVER' AS role, FALSE AS nurse
               UNION ALL SELECT 9005, 'NURSING_STAFF', TRUE) p
WHERE n.scope_id = 9001;

DROP TEMPORARY TABLE demo_night;
DROP TEMPORARY TABLE demo_floor;
DROP TEMPORARY TABLE demo_day;
