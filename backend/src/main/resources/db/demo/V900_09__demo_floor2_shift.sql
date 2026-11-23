-- Dữ liệu demo (chỉ profile demo; chủ dự án duyệt 2026-11-21): ca ngày Tầng 2 (9002) để người ở Tầng 2 (B 9002, người
-- mới tiếp nhận ở P201) phân công được Điều dưỡng, Nhân viên chăm sóc và lập kế hoạch chăm sóc (BF-01 bước 16; Điều
-- dưỡng chỉ thấy người trong phân công, Phụ lục 27). V900_02 chỉ có lịch ca Tầng 1. Gồm: Điều dưỡng thứ hai
-- demo.dieuduong2 (hồ sơ 9011, giấy phép NURSING_LICENSE), Nhân viên chăm sóc thứ hai demo.chamsoc2 (9012), mật khẩu chung
-- "Demo2026!" như V002_90 (quickstart 002 mục 1); demo.truongtang (9003) là Trưởng tầng cả Tầng 2; lịch ca tháng đã công
-- bố, ca 07:00–18:00 giờ Việt Nam (00:00–11:00 UTC) trong 7 ngày kể từ ngày nạp, dùng mẫu ca 9001 của V900_02, người phụ
-- trách ca 9011, thành viên 9011, 9012. Không phân công người cao tuổi: Trưởng tầng phân công bằng màn Lịch ca. Không ai ở
-- hai ca trùng giờ. Bảng và cách ghi như V900_02, V900_03, V900_08. Cú pháp dùng được ở MySQL 8.0 và 8.4.

SET @demo_at = UTC_TIMESTAMP(6);
SET @created_at = @demo_at - INTERVAL 1 DAY;
SET @today = DATE(@demo_at + INTERVAL 7 HOUR);
SET @manager = (SELECT id FROM account WHERE username = 'demo.quanly');

-- 002: tài khoản, vai trò, trạng thái
INSERT INTO account (username, type, staff_id, status, password_hash, must_change_password, inactivity_reference_at,
                     created_at, created_by)
VALUES ('demo.dieuduong2', 'STAFF', 9011, 'ACTIVE',
        '$argon2id$v=19$m=16384,t=2,p=1$17mqxm8hOdIjyC60bGYBQw$ja8n7hsdD4zLhrpMS2eSF1MPLbM1/KY00izjSCQkh9c',
        FALSE, @created_at, @created_at, NULL),
       ('demo.chamsoc2', 'STAFF', 9012, 'ACTIVE',
        '$argon2id$v=19$m=16384,t=2,p=1$17mqxm8hOdIjyC60bGYBQw$ja8n7hsdD4zLhrpMS2eSF1MPLbM1/KY00izjSCQkh9c',
        FALSE, @created_at, @created_at, NULL);

INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value, starts_at, actor_id, by_system,
                            details, unique_key)
SELECT 'M15.ACCOUNT', a.id, 'STATUS', 'M15', 'POINT', 'ACTIVE', @created_at, NULL, TRUE,
       JSON_OBJECT('lockCause', NULL, 'auditLogId', NULL),
       CONCAT('M15.ASH:', a.id, ':', DATE_FORMAT(@created_at, '%Y-%m-%dT%H:%i:%s.%f'))
FROM account a
WHERE a.username IN ('demo.dieuduong2', 'demo.chamsoc2');

INSERT INTO permission_assignment (kind, applies_to, account_id, role, effective_from, actor_id, reason)
SELECT 'ROLE_ASSIGNMENT', 'ACCOUNT', a.id, r.role, @created_at, NULL, 'Dữ liệu mẫu demo (ca Tầng 2, BF-01 bước 16)'
FROM account a
         JOIN (SELECT 'demo.dieuduong2' AS username, 'AC-04' AS role
               UNION ALL SELECT 'demo.chamsoc2', 'AC-05') r ON r.username = a.username;

-- 008: hồ sơ nhân viên, bộ phận, giấy phép của Điều dưỡng
INSERT INTO staff_profile (id, full_name, position, start_date, phone_number, normalized_phone, account_id,
                           created_at, created_by)
SELECT p.id, p.full_name, p.position, '2025-01-02', p.phone, p.phone, a.id, @demo_at, NULL
FROM (SELECT 9011 AS id, 'demo.dieuduong2' AS username, 'Ngô Thị Hà' AS full_name, 'Điều dưỡng' AS position,
             '0900000011' AS phone
      UNION ALL SELECT 9012, 'demo.chamsoc2', 'Trịnh Văn Long', 'Nhân viên chăm sóc', '0900000012') p
         JOIN account a ON a.username = p.username;

INSERT INTO staff_department (staff_id, department_id, starts_at, actor_id, reason)
SELECT m.staff_id, d.id, @demo_at, @manager, 'Lập hồ sơ nhân viên'
FROM (SELECT 9011 AS staff_id, 'Y tế' AS department
      UNION ALL SELECT 9012, 'Chăm sóc') m
         JOIN catalog_item d ON d.catalog_type = 'M09.DEPARTMENT' AND d.name = m.department AND d.status = 'ACTIVE';

INSERT INTO credential (staff_id, credential_type_id, credential_number, issuing_authority, issue_date, expiry_date,
                        created_at, created_by)
SELECT 9011, t.id, 'DD-DEMO-0002', 'Sở Y tế', '2024-01-15', '2029-01-15', @demo_at, @manager
FROM catalog_item t
WHERE t.catalog_type = 'M09.CREDENTIAL_TYPE' AND t.code = 'NURSING_LICENSE';

-- 008: Trưởng tầng Tầng 2
INSERT INTO staff_assignment (kind, staff_id, floor_area_id, starts_at, assigned_by, status)
VALUES ('FLOOR_MANAGER', 9003, 9002, @created_at, @manager, 'ACTIVE');

-- 008: lịch ca tháng đã công bố, ca ngày 7 ngày, người phụ trách ca, thành viên
CREATE TEMPORARY TABLE demo_day (k INT PRIMARY KEY);
INSERT INTO demo_day (k) VALUES (0), (1), (2), (3), (4), (5), (6);

INSERT IGNORE INTO shift_schedule (month, scope_type, scope_id, status)
SELECT DISTINCT DATE_FORMAT(@today + INTERVAL k DAY, '%Y-%m'), 'FLOOR', 9002, 'PUBLISHED' FROM demo_day;

INSERT INTO work_shift (schedule_id, template_id, scope_type, scope_id, starts_at, ends_at, requires_handover, status,
                        lead_staff_id)
SELECT s.id, 9001, 'FLOOR', 9002, TIMESTAMP(@today + INTERVAL d.k DAY), TIMESTAMP(@today + INTERVAL d.k DAY) + INTERVAL 11 HOUR,
       FALSE, 'PUBLISHED', 9011
FROM demo_day d
         JOIN shift_schedule s ON s.month = DATE_FORMAT(@today + INTERVAL d.k DAY, '%Y-%m') AND s.scope_type = 'FLOOR'
    AND s.scope_id = 9002
ORDER BY d.k;

CREATE TEMPORARY TABLE demo_shift AS
SELECT id, starts_at FROM work_shift WHERE template_id = 9001 AND scope_type = 'FLOOR' AND scope_id = 9002;

INSERT INTO shift_staff (shift_id, staff_id)
SELECT s.id, m.staff_id FROM demo_shift s JOIN (SELECT 9011 AS staff_id UNION ALL SELECT 9012) m;

INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value_id, starts_at, command, by_system)
SELECT 'M09.SHIFT', s.id, 'LEAD_STAFF', 'M09', 'PERIOD', 9011, s.starts_at, 'SCHEDULED', FALSE FROM demo_shift s;

DROP TEMPORARY TABLE demo_shift;
DROP TEMPORARY TABLE demo_day;
