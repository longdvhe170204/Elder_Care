-- Dữ liệu demo (chỉ profile demo; chủ dự án duyệt 2026-11-20): thêm Bác sĩ thứ hai demo.bacsi2 (hồ sơ nhân viên 9010) để
-- thử quy tắc hai người của phiếu đối chiếu thuốc trước Hoàn tất tiếp nhận (BF-01 bước 12, Q-55, BR-M07-09): trước khi
-- tiếp nhận, người cao tuổi chưa có phân công nên Điều dưỡng không thấy hồ sơ (Phụ lục 27, phạm vi theo phân công); Bác sĩ
-- toàn viện. Mật khẩu chung "Demo2026!" như V002_90 (quickstart 002 mục 1, chỉ dùng cho demo); giấy phép DOCTOR_LICENSE
-- như Bác sĩ 9004 (V900_01) để ghi, xác nhận phiếu có dòng đơn nội bộ (V900_04). Bảng theo schema sau V002_95: gán vai
-- trò ở permission_assignment, trạng thái tài khoản ở status_history (dạng AccountStore.addRole, history). Cú pháp dùng
-- được ở MySQL 8.0 và 8.4.

SET @demo_at = UTC_TIMESTAMP(6);
SET @created_at = @demo_at - INTERVAL 1 DAY;
SET @manager = (SELECT id FROM account WHERE username = 'demo.quanly');

INSERT INTO account (username, type, staff_id, status, password_hash, must_change_password, inactivity_reference_at,
                     created_at, created_by)
VALUES ('demo.bacsi2', 'STAFF', 9010, 'ACTIVE',
        '$argon2id$v=19$m=16384,t=2,p=1$17mqxm8hOdIjyC60bGYBQw$ja8n7hsdD4zLhrpMS2eSF1MPLbM1/KY00izjSCQkh9c',
        FALSE, @created_at, @created_at, NULL);

SET @doctor2 = (SELECT id FROM account WHERE username = 'demo.bacsi2');

INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value, starts_at, actor_id, by_system,
                            details, unique_key)
VALUES ('M15.ACCOUNT', @doctor2, 'STATUS', 'M15', 'POINT', 'ACTIVE', @created_at, NULL, TRUE,
        JSON_OBJECT('lockCause', NULL, 'auditLogId', NULL),
        CONCAT('M15.ASH:', @doctor2, ':', DATE_FORMAT(@created_at, '%Y-%m-%dT%H:%i:%s.%f')));

INSERT INTO permission_assignment (kind, applies_to, account_id, role, effective_from, actor_id, reason)
VALUES ('ROLE_ASSIGNMENT', 'ACCOUNT', @doctor2, 'AC-03', @created_at, NULL,
        'Dữ liệu mẫu demo (Bác sĩ thứ hai, BF-01 bước 12)');

INSERT INTO staff_profile (id, full_name, position, start_date, phone_number, normalized_phone, account_id,
                           created_at, created_by)
VALUES (9010, 'Đặng Minh Khoa', 'Bác sĩ', '2025-01-02', '0900000010', '0900000010', @doctor2, @demo_at, NULL);

INSERT INTO staff_department (staff_id, department_id, starts_at, actor_id, reason)
SELECT 9010, d.id, @demo_at, @manager, 'Lập hồ sơ nhân viên'
FROM catalog_item d
WHERE d.catalog_type = 'M09.DEPARTMENT' AND d.name = 'Y tế' AND d.status = 'ACTIVE';

INSERT INTO credential (staff_id, credential_type_id, credential_number, issuing_authority, issue_date, expiry_date,
                        created_at, created_by)
SELECT 9010, t.id, 'BS-DEMO-0002', 'Sở Y tế', '2024-01-15', '2029-01-15', @demo_at, @manager
FROM catalog_item t
WHERE t.catalog_type = 'M09.CREDENTIAL_TYPE' AND t.code = 'DOCTOR_LICENSE';
