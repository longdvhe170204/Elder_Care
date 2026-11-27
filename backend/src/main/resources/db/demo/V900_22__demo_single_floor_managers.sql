-- Dữ liệu demo Q-453: mỗi Trưởng tầng phụ trách một tầng để thử chép công việc theo tháng.
-- Giữ demo.truongtang (9003) ở Tầng 1 Nhà A và demo.truongtang2 (9013) ở Tầng 2 Nhà A.
-- Bổ sung Trưởng tầng riêng cho ba tầng mới của V900_14, giữ lịch ca và Phụ trách bộ phận của V900_15.

SET @demo_at = UTC_TIMESTAMP(6);
SET @created_at = @demo_at - INTERVAL 1 DAY;
SET @manager = (SELECT id FROM account WHERE username = 'demo.quanly');
SET @hash = '$argon2id$v=19$m=16384,t=2,p=1$17mqxm8hOdIjyC60bGYBQw$ja8n7hsdD4zLhrpMS2eSF1MPLbM1/KY00izjSCQkh9c';

CREATE TEMPORARY TABLE demo_floor_manager (
    staff_id BIGINT PRIMARY KEY, floor_area_id BIGINT NOT NULL, username VARCHAR(40) NOT NULL,
    full_name VARCHAR(100) NOT NULL, phone VARCHAR(12) NOT NULL);
INSERT INTO demo_floor_manager VALUES
    (9023, 9003, 'demo.truongtang3', 'Lê Hoàng Nam', '0900000023'),
    (9024, 9004, 'demo.truongtang4', 'Trần Văn Minh', '0900000024'),
    (9025, 9005, 'demo.truongtang5', 'Nguyễn Thị Hà', '0900000025');

INSERT INTO account (username, type, staff_id, status, password_hash, must_change_password, inactivity_reference_at,
                     created_at, created_by)
SELECT username, 'STAFF', staff_id, 'ACTIVE', @hash, FALSE, @created_at, @created_at, NULL
FROM demo_floor_manager;

INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value, starts_at, actor_id, by_system,
                            details, unique_key)
SELECT 'M15.ACCOUNT', a.id, 'STATUS', 'M15', 'POINT', 'ACTIVE', @created_at, NULL, TRUE,
       JSON_OBJECT('lockCause', NULL, 'auditLogId', NULL),
       CONCAT('M15.ASH:', a.id, ':', DATE_FORMAT(@created_at, '%Y-%m-%dT%H:%i:%s.%f'))
FROM account a JOIN demo_floor_manager s ON s.username = a.username;

INSERT INTO permission_assignment (kind, applies_to, account_id, role, effective_from, actor_id, reason)
SELECT 'ROLE_ASSIGNMENT', 'ACCOUNT', a.id, 'AC-02', @created_at, NULL, 'Dữ liệu mẫu demo (Trưởng tầng, Q-453)'
FROM account a JOIN demo_floor_manager s ON s.username = a.username;

INSERT INTO staff_profile (id, full_name, position, start_date, phone_number, normalized_phone, account_id,
                           created_at, created_by)
SELECT s.staff_id, s.full_name, 'Trưởng tầng', '2025-01-02', s.phone, s.phone, a.id, @demo_at, NULL
FROM demo_floor_manager s JOIN account a ON a.username = s.username;

INSERT INTO staff_department (staff_id, department_id, starts_at, actor_id, reason)
SELECT s.staff_id, d.id, @demo_at, @manager, 'Lập hồ sơ nhân viên'
FROM demo_floor_manager s
JOIN catalog_item d ON d.catalog_type = 'M09.DEPARTMENT' AND d.name = 'Chăm sóc' AND d.status = 'ACTIVE';

UPDATE staff_assignment
SET status = 'ENDED', ends_at = GREATEST(@demo_at, starts_at),
    end_reason = 'Dữ liệu demo Q-453: mỗi Trưởng tầng một tầng'
WHERE kind = 'FLOOR_MANAGER' AND status = 'ACTIVE'
  AND ((staff_id = 9003 AND floor_area_id IN (9002, 9003))
       OR (staff_id = 9013 AND floor_area_id IN (9004, 9005)));

INSERT INTO staff_assignment (kind, staff_id, floor_area_id, starts_at, assigned_by, status)
VALUES ('FLOOR_MANAGER', 9013, 9002, @demo_at, @manager, 'ACTIVE');

INSERT INTO staff_assignment (kind, staff_id, floor_area_id, starts_at, assigned_by, status)
SELECT 'FLOOR_MANAGER', staff_id, floor_area_id, @demo_at, @manager, 'ACTIVE'
FROM demo_floor_manager;

DROP TEMPORARY TABLE demo_floor_manager;
