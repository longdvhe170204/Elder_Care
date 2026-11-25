-- Dữ liệu demo (chỉ profile demo; chủ dự án duyệt 2026-11-25: dữ liệu demo đầy đủ để chạy hết quy trình của viện):
-- nhân sự và ca cho hạ tầng của V900_14, ca đủ dài để thử và demo nhiều ngày (V900_02, V900_09, V900_11 chỉ có 7 ngày).
--   1. Bảy nhân viên mới, mật khẩu chung "Demo2026!" như V002_90 (quickstart 002 mục 1): demo.truongtang2 (9013, Trưởng
--      tầng Nhà B), demo.dieuduong3/4/5 (9014 → 9016, giấy phép NURSING_LICENSE), demo.chamsoc3/4/5 (9017 → 9019);
--      tài khoản, vai trò, trạng thái như V900_08, V900_09.
--   2. Trưởng tầng: demo.truongtang (9003) thêm Tầng 3 Nhà A; demo.truongtang2 Tầng 1, Tầng 2 Nhà B.
--   3. Phụ trách bộ phận (008, Q-284 luôn có; thiếu thì yêu cầu mua sắm 019 bị chặn DEPARTMENT_LEAD_MISSING): Hành chính –
--      Kế toán demo.hanhchinh, Y tế demo.bacsi, Chăm sóc demo.truongtang, Bếp demo.bep; ghi như DepartmentDutyService.
--      assignLead (leadStaffId của bộ phận, dòng lịch sử M09.DEPARTMENT LEAD_STAFF, mã bộ phận logic theo legacy).
--   4. Lịch ca tháng đã công bố, ca ngày 07:00–18:00 (mẫu 9001) và ca đêm 18:00–07:00 (mẫu "Ca đêm" của V900_11) cho
--      năm tầng từ ngày nạp tới hết tháng sau (hai tầng cũ nối tiếp từ ngày thứ 8, sau 7 ngày của V900_02, 09, 11);
--      người phụ trách ca là Điều dưỡng của tầng, thành viên thêm Nhân viên chăm sóc của tầng; Bác sĩ trực demo.bacsi ở
--      Tầng 1 Nhà A, demo.bacsi2 ở Tầng 1 Nhà B (một người không ở hai ca trùng giờ).
-- Phân công người cao tuổi trong ca: V900_17. Cú pháp dùng được ở MySQL 8.0 và 8.4.

SET @demo_at = UTC_TIMESTAMP(6);
SET @created_at = @demo_at - INTERVAL 1 DAY;
SET @today = DATE(@demo_at + INTERVAL 7 HOUR);
SET @last_day = LAST_DAY(@today + INTERVAL 1 MONTH);
SET @manager = (SELECT id FROM account WHERE username = 'demo.quanly');
SET @night_template = (SELECT id FROM catalog_item
                       WHERE catalog_type = 'M09.SHIFT_TEMPLATE' AND name = 'Ca đêm' ORDER BY id LIMIT 1);
SET @hash = '$argon2id$v=19$m=16384,t=2,p=1$17mqxm8hOdIjyC60bGYBQw$ja8n7hsdD4zLhrpMS2eSF1MPLbM1/KY00izjSCQkh9c';

-- 002, 008: nhân viên mới
CREATE TEMPORARY TABLE demo_staff (
    id BIGINT PRIMARY KEY, username VARCHAR(40), full_name VARCHAR(100), position VARCHAR(60), phone VARCHAR(12),
    role VARCHAR(8), department VARCHAR(60), license VARCHAR(40), license_number VARCHAR(20));
INSERT INTO demo_staff VALUES
    (9013, 'demo.truongtang2', 'Phan Văn Hùng', 'Trưởng tầng', '0900000013', 'AC-02', 'Chăm sóc', NULL, NULL),
    (9014, 'demo.dieuduong3', 'Lý Thị Mai', 'Điều dưỡng', '0900000014', 'AC-04', 'Y tế', 'NURSING_LICENSE',
     'DD-DEMO-0003'),
    (9015, 'demo.dieuduong4', 'Đinh Thị Lan', 'Điều dưỡng', '0900000015', 'AC-04', 'Y tế', 'NURSING_LICENSE',
     'DD-DEMO-0004'),
    (9016, 'demo.dieuduong5', 'Tạ Văn Nam', 'Điều dưỡng', '0900000016', 'AC-04', 'Y tế', 'NURSING_LICENSE',
     'DD-DEMO-0005'),
    (9017, 'demo.chamsoc3', 'Mai Thị Hoa', 'Nhân viên chăm sóc', '0900000017', 'AC-05', 'Chăm sóc', NULL, NULL),
    (9018, 'demo.chamsoc4', 'Cao Văn Sơn', 'Nhân viên chăm sóc', '0900000018', 'AC-05', 'Chăm sóc', NULL, NULL),
    (9019, 'demo.chamsoc5', 'Kiều Thị Thu', 'Nhân viên chăm sóc', '0900000019', 'AC-05', 'Chăm sóc', NULL, NULL);

INSERT INTO account (username, type, staff_id, status, password_hash, must_change_password, inactivity_reference_at,
                     created_at, created_by)
SELECT s.username, 'STAFF', s.id, 'ACTIVE', @hash, FALSE, @created_at, @created_at, NULL FROM demo_staff s;

INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value, starts_at, actor_id, by_system,
                            details, unique_key)
SELECT 'M15.ACCOUNT', a.id, 'STATUS', 'M15', 'POINT', 'ACTIVE', @created_at, NULL, TRUE,
       JSON_OBJECT('lockCause', NULL, 'auditLogId', NULL),
       CONCAT('M15.ASH:', a.id, ':', DATE_FORMAT(@created_at, '%Y-%m-%dT%H:%i:%s.%f'))
FROM account a JOIN demo_staff s ON s.username = a.username;

INSERT INTO permission_assignment (kind, applies_to, account_id, role, effective_from, actor_id, reason)
SELECT 'ROLE_ASSIGNMENT', 'ACCOUNT', a.id, s.role, @created_at, NULL, 'Dữ liệu mẫu demo (nhân sự đầy đủ)'
FROM account a JOIN demo_staff s ON s.username = a.username;

INSERT INTO staff_profile (id, full_name, position, start_date, phone_number, normalized_phone, account_id,
                           created_at, created_by)
SELECT s.id, s.full_name, s.position, '2025-01-02', s.phone, s.phone, a.id, @demo_at, NULL
FROM demo_staff s JOIN account a ON a.username = s.username;

INSERT INTO staff_department (staff_id, department_id, starts_at, actor_id, reason)
SELECT s.id, d.id, @demo_at, @manager, 'Lập hồ sơ nhân viên'
FROM demo_staff s
         JOIN catalog_item d ON d.catalog_type = 'M09.DEPARTMENT' AND d.name = s.department AND d.status = 'ACTIVE';

INSERT INTO credential (staff_id, credential_type_id, credential_number, issuing_authority, issue_date, expiry_date,
                        created_at, created_by)
SELECT s.id, t.id, s.license_number, 'Sở Y tế', '2024-01-15', '2029-01-15', @demo_at, @manager
FROM demo_staff s JOIN catalog_item t ON t.catalog_type = 'M09.CREDENTIAL_TYPE' AND t.code = s.license
WHERE s.license IS NOT NULL;

DROP TEMPORARY TABLE demo_staff;

-- 008: Trưởng tầng các tầng mới
INSERT INTO staff_assignment (kind, staff_id, floor_area_id, starts_at, assigned_by, status) VALUES
    ('FLOOR_MANAGER', 9003, 9003, @created_at, @manager, 'ACTIVE'),
    ('FLOOR_MANAGER', 9013, 9004, @created_at, @manager, 'ACTIVE'),
    ('FLOOR_MANAGER', 9013, 9005, @created_at, @manager, 'ACTIVE');

-- 008: Phụ trách bộ phận
CREATE TEMPORARY TABLE demo_lead (department VARCHAR(60) PRIMARY KEY, staff_id BIGINT);
INSERT INTO demo_lead VALUES
    ('Hành chính – Kế toán', 9008), ('Y tế', 9004), ('Chăm sóc', 9003), ('Bếp', 9007);

UPDATE catalog_item d JOIN demo_lead l ON l.department = d.name
SET d.attributes = JSON_SET(d.attributes, '$.leadStaffId', l.staff_id), d.version = d.version + 1
WHERE d.catalog_type = 'M09.DEPARTMENT' AND d.status = 'ACTIVE';

INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value_id, starts_at, command, actor_id,
                            by_system, reason, details)
SELECT 'M09.DEPARTMENT', COALESCE(CAST(d.attributes ->> '$.legacy.id' AS UNSIGNED), d.id), 'LEAD_STAFF', 'M09',
       'PERIOD', l.staff_id, @created_at, 'LEAD_CHANGED', @manager, FALSE, 'Phân công Phụ trách bộ phận (dữ liệu demo)',
       JSON_OBJECT('legacy', NULL)
FROM catalog_item d JOIN demo_lead l ON l.department = d.name
WHERE d.catalog_type = 'M09.DEPARTMENT' AND d.status = 'ACTIVE';

DROP TEMPORARY TABLE demo_lead;

-- 008: lịch ca từ ngày nạp tới hết tháng sau
CREATE TEMPORARY TABLE demo_day (k INT PRIMARY KEY);
INSERT INTO demo_day (k)
WITH RECURSIVE d (k) AS (SELECT 0 UNION ALL SELECT k + 1 FROM d WHERE k < DATEDIFF(@last_day, @today))
SELECT k FROM d;

-- Tầng, Điều dưỡng phụ trách ca, Nhân viên chăm sóc, Bác sĩ trực, ngày bắt đầu (hai tầng cũ đã có 7 ngày)
CREATE TEMPORARY TABLE demo_floor (
    floor_id BIGINT PRIMARY KEY, lead_id BIGINT, member_id BIGINT, doctor_id BIGINT NULL, from_day INT);
INSERT INTO demo_floor VALUES
    (9001, 9005, 9006, 9004, 7), (9002, 9011, 9012, NULL, 7), (9003, 9014, 9017, NULL, 0),
    (9004, 9015, 9018, 9010, 0), (9005, 9016, 9019, NULL, 0);

CREATE TEMPORARY TABLE demo_template (template_id BIGINT PRIMARY KEY, start_hour INT, hours INT);
INSERT INTO demo_template VALUES (9001, 0, 11), (@night_template, 11, 13);

INSERT IGNORE INTO shift_schedule (month, scope_type, scope_id, status)
SELECT DISTINCT DATE_FORMAT(@today + INTERVAL d.k DAY, '%Y-%m'), 'FLOOR', f.floor_id, 'PUBLISHED'
FROM demo_day d JOIN demo_floor f ON d.k >= f.from_day;

INSERT INTO work_shift (schedule_id, template_id, scope_type, scope_id, starts_at, ends_at, requires_handover, status,
                        lead_staff_id)
SELECT s.id, t.template_id, 'FLOOR', f.floor_id,
       TIMESTAMP(@today + INTERVAL d.k DAY) + INTERVAL t.start_hour HOUR,
       TIMESTAMP(@today + INTERVAL d.k DAY) + INTERVAL (t.start_hour + t.hours) HOUR,
       FALSE, 'PUBLISHED', f.lead_id
FROM demo_day d
         JOIN demo_floor f ON d.k >= f.from_day
         JOIN demo_template t
         JOIN shift_schedule s ON s.month = DATE_FORMAT(@today + INTERVAL d.k DAY, '%Y-%m') AND s.scope_type = 'FLOOR'
    AND s.scope_id = f.floor_id
ORDER BY f.floor_id, d.k, t.start_hour;

CREATE TEMPORARY TABLE demo_shift AS
SELECT w.id, w.scope_id, w.starts_at, f.lead_id, f.member_id, f.doctor_id
FROM work_shift w JOIN demo_floor f ON f.floor_id = w.scope_id
WHERE w.scope_type = 'FLOOR'
  AND w.starts_at >= TIMESTAMP(@today + INTERVAL f.from_day DAY)
  AND w.template_id IN (9001, @night_template);

-- Bảng tạm của MySQL không đọc hai lần trong một câu lệnh: mỗi loại thành viên một lệnh
INSERT INTO shift_staff (shift_id, staff_id) SELECT s.id, s.lead_id FROM demo_shift s;
INSERT INTO shift_staff (shift_id, staff_id) SELECT s.id, s.member_id FROM demo_shift s;
INSERT INTO shift_staff (shift_id, staff_id, on_duty_doctor)
SELECT s.id, s.doctor_id, TRUE FROM demo_shift s WHERE s.doctor_id IS NOT NULL;

INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value_id, starts_at, command, by_system)
SELECT 'M09.SHIFT', s.id, 'LEAD_STAFF', 'M09', 'PERIOD', s.lead_id, s.starts_at, 'SCHEDULED', FALSE FROM demo_shift s;

DROP TEMPORARY TABLE demo_shift;
DROP TEMPORARY TABLE demo_template;
DROP TEMPORARY TABLE demo_floor;
DROP TEMPORARY TABLE demo_day;
