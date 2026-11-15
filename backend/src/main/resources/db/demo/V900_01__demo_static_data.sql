-- Dữ liệu demo tĩnh (chủ dự án yêu cầu 2026-11-13; quickstart 002 mục 1): chỉ nạp ở profile demo
-- (spring.flyway.locations thêm classpath:db/demo), không nạp ở test hay khi chạy thật.
-- Gồm: cơ cấu một khu, hai tầng, bốn phòng; hồ sơ nhân viên 9001 → 9009 khớp chín tài khoản demo.* (V002_90), bộ phận,
-- giấy phép; hai người cao tuổi ở trạng thái Đang tiếp nhận; hai người thân của người thứ nhất.
-- Không gồm: giường (lệnh tạo giường còn ghi lịch sử trạng thái và ghi tăng tài sản, tạo bằng ứng dụng), ca, phân công,
-- hợp đồng, phân bổ giường và mọi dữ liệu có máy trạng thái: tạo bằng thao tác trên ứng dụng.
-- Id đặt từ 9001 để không trùng dữ liệu đã có; dòng lịch sử ghi giống lệnh của ứng dụng.

SET @demo_at = UTC_TIMESTAMP(6);
SET @office = (SELECT id FROM account WHERE username = 'demo.hanhchinh');
SET @manager = (SELECT id FROM account WHERE username = 'demo.quanly');

-- 003: cơ cấu
INSERT INTO area (id, code, name) VALUES (9001, 'KA', 'Khu A');

INSERT INTO floor (id, area_id, code, name, building_name) VALUES
    (9001, 9001, 'T1', 'Tầng 1', 'Nhà A'),
    (9002, 9001, 'T2', 'Tầng 2', 'Nhà A');

INSERT INTO room_type (id, name, standard_beds, serves_long_term, serves_short_term, allowed_care_levels) VALUES
    (9001, 'Phòng đôi', 2, TRUE, TRUE, JSON_ARRAY('BASIC', 'FREQUENT', 'SPECIAL')),
    (9002, 'Phòng bốn giường', 4, TRUE, TRUE, JSON_ARRAY('BASIC', 'FREQUENT'));

INSERT INTO room (id, floor_id, room_type_id, code, name, gender_policy) VALUES
    (9001, 9001, 9001, 'P101', 'Phòng 101', 'FEMALE'),
    (9002, 9001, 9002, 'P102', 'Phòng 102', 'MALE'),
    (9003, 9002, 9001, 'P201', 'Phòng 201', 'UNRESTRICTED'),
    (9004, 9002, 9002, 'P202', 'Phòng 202', 'UNRESTRICTED');

-- 008: hồ sơ nhân viên của chín tài khoản demo.*
INSERT INTO staff_profile (id, full_name, position, start_date, phone_number, normalized_phone, account_id,
                           created_at, created_by)
SELECT s.id, s.full_name, s.position, '2025-01-02', s.phone, s.phone, a.id, @demo_at, NULL
FROM (SELECT 9001 AS id, 'demo.quanly' AS username, 'Nguyễn Minh Quân' AS full_name, 'Quản lý viện' AS position,
             '0900000001' AS phone
      UNION ALL SELECT 9002, 'demo.quanly2', 'Lê Thu Hà', 'Quản lý viện', '0900000002'
      UNION ALL SELECT 9003, 'demo.truongtang', 'Trần Quốc Tuấn', 'Trưởng tầng', '0900000003'
      UNION ALL SELECT 9004, 'demo.bacsi', 'Phạm Đức Bình', 'Bác sĩ', '0900000004'
      UNION ALL SELECT 9005, 'demo.dieuduong', 'Hoàng Thị Dung', 'Điều dưỡng', '0900000005'
      UNION ALL SELECT 9006, 'demo.chamsoc', 'Vũ Văn Cường', 'Nhân viên chăm sóc', '0900000006'
      UNION ALL SELECT 9007, 'demo.bep', 'Đỗ Thị Bích', 'Nhân viên bếp', '0900000007'
      UNION ALL SELECT 9008, 'demo.hanhchinh', 'Bùi Thị Hạnh', 'Hành chính', '0900000008'
      UNION ALL SELECT 9009, 'demo.ketoan', 'Ngô Văn Kiên', 'Kế toán', '0900000009') s
         JOIN account a ON a.username = s.username;

INSERT INTO staff_department (staff_id, department_id, starts_at, actor_id, reason)
SELECT s.staff_id, d.id, @demo_at, @manager, 'Lập hồ sơ nhân viên'
FROM (SELECT 9001 AS staff_id, 'Hành chính – Kế toán' AS department
      UNION ALL SELECT 9002, 'Hành chính – Kế toán'
      UNION ALL SELECT 9003, 'Chăm sóc'
      UNION ALL SELECT 9004, 'Y tế'
      UNION ALL SELECT 9005, 'Y tế'
      UNION ALL SELECT 9006, 'Chăm sóc'
      UNION ALL SELECT 9007, 'Bếp'
      UNION ALL SELECT 9008, 'Hành chính – Kế toán'
      UNION ALL SELECT 9009, 'Hành chính – Kế toán') s
         JOIN catalog_item d ON d.catalog_type = 'M09.DEPARTMENT' AND d.name = s.department AND d.status = 'ACTIVE';

INSERT INTO credential (staff_id, credential_type_id, credential_number, issuing_authority, issue_date, expiry_date,
                        created_at, created_by)
SELECT c.staff_id, t.id, c.credential_number, 'Sở Y tế', '2024-01-15', '2029-01-15', @demo_at, @manager
FROM (SELECT 9004 AS staff_id, 'DOCTOR_LICENSE' AS type_code, 'BS-DEMO-0001' AS credential_number
      UNION ALL SELECT 9005, 'NURSING_LICENSE', 'DD-DEMO-0001') c
         JOIN catalog_item t ON t.catalog_type = 'M09.CREDENTIAL_TYPE' AND t.code = c.type_code;

-- 001: hai hồ sơ người cao tuổi ở Đang tiếp nhận, kèm dòng lịch sử trạng thái như lệnh Tạo hồ sơ
INSERT INTO resident (id, profile_code, full_name, date_of_birth, gender, permanent_address, status, status_since,
                      created_at, created_by) VALUES
    (9001, 'NCT-2026-009001', 'Nguyễn Thị An', '1944-05-20', 'FEMALE', 'Quận Cầu Giấy, Hà Nội',
     'ADMISSION_IN_PROGRESS', @demo_at, @demo_at, @office),
    (9002, 'NCT-2026-009002', 'Trần Văn Bình', '1940-11-02', 'MALE', 'Quận Đống Đa, Hà Nội',
     'ADMISSION_IN_PROGRESS', @demo_at, @demo_at, @office);

INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value, starts_at, command, actor_id,
                            by_system, details)
SELECT 'M01.RESIDENT', r.id, 'STATUS', 'M01', 'POINT', 'ADMISSION_IN_PROGRESS', @demo_at, 'CREATE_PROFILE', @office,
       FALSE, JSON_OBJECT('conditionWaiver', NULL)
FROM resident r
WHERE r.id IN (9001, 9002);

-- 012: người thân R (người đại diện, liên hệ chính, xem sức khỏe) và M (xem chi phí) của người cao tuổi 9001
INSERT INTO family_member (id, full_name, phone_number, address, created_by, created_at) VALUES
    (9001, 'Nguyễn Văn Rạng', '0911000001', 'Quận Cầu Giấy, Hà Nội', @office, @demo_at),
    (9002, 'Nguyễn Thị Mơ', '0911000002', 'Quận Ba Đình, Hà Nội', @office, @demo_at);

INSERT INTO family_relationship (id, family_member_id, resident_id, relationship, representative, primary_contact,
                                 contact_order, view_health, view_costs, receive_urgent, register_visits,
                                 pickup_allowed, request_services, started_at, created_by, created_at) VALUES
    (9001, 9001, 9001, 'CHILD', TRUE, TRUE, 1, TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, @demo_at, @office, @demo_at),
    (9002, 9002, 9001, 'CHILD', FALSE, FALSE, 2, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, @demo_at, @office,
     @demo_at);

INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value, starts_at, command, actor_id,
                            by_system)
SELECT 'M10.FAMILY_RELATIONSHIP', h.relationship_id, h.attribute, 'M10', 'POINT', 'true', @demo_at, 'CREATE', @office,
       FALSE
FROM (SELECT 9001 AS relationship_id, 'REPRESENTATIVE' AS attribute
      UNION ALL SELECT 9001, 'PRIMARY_CONTACT'
      UNION ALL SELECT 9001, 'VIEW_HEALTH'
      UNION ALL SELECT 9001, 'RECEIVE_URGENT'
      UNION ALL SELECT 9001, 'REGISTER_VISITS'
      UNION ALL SELECT 9001, 'PICKUP_ALLOWED'
      UNION ALL SELECT 9001, 'REQUEST_SERVICES'
      UNION ALL SELECT 9002, 'VIEW_COSTS') h;
