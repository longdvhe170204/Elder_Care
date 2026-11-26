-- Dữ liệu demo (chỉ profile demo; chủ dự án duyệt 2026-11-26, Q-453): mỗi Trưởng tầng phụ trách một tầng. V900_09 cho
-- demo.truongtang (9003) làm Trưởng tầng cả Tầng 2, nên màn "Công việc tháng" báo phụ trách nhiều tầng và không chép được.
-- Thêm Trưởng tầng Tầng 2 demo.truongtang2 (hồ sơ 9013), mật khẩu chung "Demo2026!" như V002_90; kết thúc phân công Tầng 2
-- của 9003 (V900_09 đã chạy nên không sửa file đó). Bảng và cách ghi như V900_09. Cú pháp dùng được ở MySQL 8.0 và 8.4.

SET @demo_at = UTC_TIMESTAMP(6);
SET @created_at = @demo_at - INTERVAL 1 DAY;
SET @manager = (SELECT id FROM account WHERE username = 'demo.quanly');

-- 002: tài khoản, vai trò, trạng thái
INSERT INTO account (username, type, staff_id, status, password_hash, must_change_password, inactivity_reference_at,
                     created_at, created_by)
VALUES ('demo.truongtang2', 'STAFF', 9013, 'ACTIVE',
        '$argon2id$v=19$m=16384,t=2,p=1$17mqxm8hOdIjyC60bGYBQw$ja8n7hsdD4zLhrpMS2eSF1MPLbM1/KY00izjSCQkh9c',
        FALSE, @created_at, @created_at, NULL);

INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value, starts_at, actor_id, by_system,
                            details, unique_key)
SELECT 'M15.ACCOUNT', a.id, 'STATUS', 'M15', 'POINT', 'ACTIVE', @created_at, NULL, TRUE,
       JSON_OBJECT('lockCause', NULL, 'auditLogId', NULL),
       CONCAT('M15.ASH:', a.id, ':', DATE_FORMAT(@created_at, '%Y-%m-%dT%H:%i:%s.%f'))
FROM account a
WHERE a.username = 'demo.truongtang2';

INSERT INTO permission_assignment (kind, applies_to, account_id, role, effective_from, actor_id, reason)
SELECT 'ROLE_ASSIGNMENT', 'ACCOUNT', a.id, 'AC-02', @created_at, NULL, 'Dữ liệu mẫu demo (Trưởng tầng Tầng 2, Q-453)'
FROM account a
WHERE a.username = 'demo.truongtang2';

-- 008: hồ sơ nhân viên, bộ phận
INSERT INTO staff_profile (id, full_name, position, start_date, phone_number, normalized_phone, account_id,
                           created_at, created_by)
SELECT 9013, 'Lê Hoàng Nam', 'Trưởng tầng', '2025-01-02', '0900000013', '0900000013', a.id, @demo_at, NULL
FROM account a
WHERE a.username = 'demo.truongtang2';

INSERT INTO staff_department (staff_id, department_id, starts_at, actor_id, reason)
SELECT 9013, d.id, @demo_at, @manager, 'Lập hồ sơ nhân viên'
FROM catalog_item d
WHERE d.catalog_type = 'M09.DEPARTMENT' AND d.name = 'Chăm sóc' AND d.status = 'ACTIVE';

-- 008: kết thúc phân công Trưởng tầng Tầng 2 của 9003, giao Tầng 2 cho 9013
UPDATE staff_assignment
SET status     = 'ENDED',
    ends_at    = GREATEST(@demo_at, starts_at),
    end_reason = 'Dữ liệu demo: mỗi Trưởng tầng một tầng (Q-453)'
WHERE kind = 'FLOOR_MANAGER' AND staff_id = 9003 AND floor_area_id = 9002 AND status = 'ACTIVE';

INSERT INTO staff_assignment (kind, staff_id, floor_area_id, starts_at, assigned_by, status)
VALUES ('FLOOR_MANAGER', 9013, 9002, @demo_at, @manager, 'ACTIVE');
