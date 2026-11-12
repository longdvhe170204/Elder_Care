-- Dữ liệu nền cho nghiệm thu quickstart 007, 009, 010, 011 trên schema demo riêng (elder_care_qs3), nạp một lần sau khi
-- Flyway của profile demo chạy xong. Ghi thẳng vào bảng như fixture test của chủ module (support/RealCareFixture,
-- family/RelationshipFixtures): demo chưa có dữ liệu mẫu của 001, 003, 004, 008, 012. Không dùng cho schema elder_care.
-- Mốc thời gian: đồng hồ giả lập đặt 2026-12-01 08:00 +07:00 (thứ 3); ca ngày 07:00–18:00, ca đêm 18:00–07:00 tầng 2.
-- Cột thời gian lưu UTC (R-04).

-- Tài khoản thêm (mật khẩu demo chung như demo/V002_90)
INSERT IGNORE INTO account (username, type, staff_id, status, password_hash, must_change_password, inactivity_reference_at,
                     created_at, created_by)
SELECT s.username, 'STAFF', s.staff_id, 'ACTIVE',
       '$argon2id$v=19$m=16384,t=2,p=1$17mqxm8hOdIjyC60bGYBQw$ja8n7hsdD4zLhrpMS2eSF1MPLbM1/KY00izjSCQkh9c',
       FALSE, '2026-11-01 00:00:00', '2026-11-01 00:00:00', NULL
FROM (SELECT 'demo.dieuduong2' AS username, 9010 AS staff_id
      UNION ALL SELECT 'demo.dieuduong5', 9011
      UNION ALL SELECT 'demo.phutrachca', 9012
      UNION ALL SELECT 'demo.bacsi1', 9013
      UNION ALL SELECT 'demo.dieuduongdem', 9014
      UNION ALL SELECT 'demo.bep2', 9015
      UNION ALL SELECT 'demo.hanhchinh2', 9016) s;
INSERT INTO permission_assignment (kind, applies_to, account_id, role, effective_from, actor_id, reason)
SELECT 'ROLE_ASSIGNMENT', 'ACCOUNT', a.id, r.role, '2026-11-01 00:00:00', NULL, 'Dữ liệu nghiệm thu quickstart'
FROM account a
         JOIN (SELECT 9010 AS staff_id, 'AC-04' AS role UNION ALL SELECT 9011, 'AC-04' UNION ALL SELECT 9012, 'AC-04'
               UNION ALL SELECT 9013, 'AC-03' UNION ALL SELECT 9014, 'AC-04' UNION ALL SELECT 9015, 'AC-07'
               UNION ALL SELECT 9016, 'AC-09') r ON r.staff_id = a.staff_id;
-- Tài khoản demo của V002_90 lập lúc chạy Flyway: lùi mốc tạo, mốc hoạt động về 01/11 cho khớp đồng hồ giả lập
UPDATE account SET created_at = '2026-11-01 00:00:00', inactivity_reference_at = '2026-11-01 00:00:00'
WHERE username LIKE 'demo.%';
UPDATE permission_assignment SET effective_from = '2026-11-01 00:00:00' WHERE kind = 'ROLE_ASSIGNMENT' AND account_id IN (SELECT id FROM account WHERE username LIKE 'demo.%');

-- Hồ sơ nhân viên 008
INSERT INTO staff_profile (id, full_name, position, account_id)
SELECT a.staff_id, n.full_name, n.position, a.id
FROM account a JOIN (
    SELECT 9001 AS staff_id, 'Quản lý viện QL' AS full_name, 'NURSING_HOME_MANAGER' AS position
    UNION ALL SELECT 9002, 'Quản lý viện QL2', 'NURSING_HOME_MANAGER'
    UNION ALL SELECT 9003, 'Trưởng tầng T', 'FLOOR_MANAGER'
    UNION ALL SELECT 9004, 'Bác sĩ B', 'DOCTOR'
    UNION ALL SELECT 9005, 'Điều dưỡng D', 'NURSING_STAFF'
    UNION ALL SELECT 9006, 'Nhân viên chăm sóc S', 'CAREGIVER'
    UNION ALL SELECT 9007, 'Nhân viên bếp K', 'KITCHEN_STAFF'
    UNION ALL SELECT 9008, 'Hành chính H', 'OFFICE_STAFF'
    UNION ALL SELECT 9009, 'Kế toán KT', 'ACCOUNTANT'
    UNION ALL SELECT 9010, 'Điều dưỡng D2', 'NURSING_STAFF'
    UNION ALL SELECT 9011, 'Điều dưỡng D5', 'NURSING_STAFF'
    UNION ALL SELECT 9012, 'Người phụ trách ca P', 'NURSING_STAFF'
    UNION ALL SELECT 9013, 'Bác sĩ BS1', 'DOCTOR'
    UNION ALL SELECT 9014, 'Điều dưỡng ca đêm Đ', 'NURSING_STAFF'
    UNION ALL SELECT 9015, 'Nhân viên bếp B2', 'KITCHEN_STAFF'
    UNION ALL SELECT 9016, 'Hành chính HC2', 'OFFICE_STAFF') n ON n.staff_id = a.staff_id
WHERE a.staff_id IS NOT NULL;

-- Khu, tầng 2, tầng 3, phòng, giường (003)
INSERT INTO area (id, code, name) VALUES (1001, 'KA', 'Khu A');
INSERT INTO floor (id, area_id, code, name) VALUES (1002, 1001, 'T2', 'Tầng 2'), (1003, 1001, 'T3', 'Tầng 3');
INSERT INTO room_type (id, name, standard_beds) VALUES (1004, 'Phòng hai giường', 2);
INSERT INTO room_type_care_level VALUES (1004, 'BASIC');
INSERT INTO room (id, floor_id, room_type_id, code, name, gender_policy) VALUES
    (1011, 1002, 1004, 'P201', 'Phòng 201', 'UNRESTRICTED'),
    (1012, 1002, 1004, 'P202', 'Phòng 202', 'UNRESTRICTED'),
    (1013, 1002, 1004, 'P203', 'Phòng 203', 'UNRESTRICTED'),
    (1021, 1003, 1004, 'P301', 'Phòng 301', 'UNRESTRICTED');
UPDATE room SET allowed_care_levels = JSON_ARRAY('BASIC') WHERE id IN (1011, 1012, 1013, 1021);
INSERT INTO bed (id, room_id, code, state) VALUES
    (1101, 1011, 'G1', 'OCCUPIED'), (1102, 1011, 'G2', 'OCCUPIED'),
    (1103, 1012, 'G1', 'OCCUPIED'), (1104, 1012, 'G2', 'OCCUPIED'),
    (1105, 1013, 'G1', 'EMPTY'), (1106, 1013, 'G2', 'EMPTY'),
    (1107, 1021, 'G1', 'OCCUPIED'), (1108, 1021, 'G2', 'EMPTY');

-- Người cao tuổi (001): A, E ở P201; C, F ở P202; G tầng 3; B bán trú (không giường)
INSERT INTO resident (id, profile_code, full_name, date_of_birth, gender, status, status_since, current_care_level,
                      created_at, created_by) VALUES
    (2001, 'QS-A', 'Nguyễn Thị A', '1940-05-01', 'FEMALE', 'IN_RESIDENCE', '2026-11-01 01:00:00', 'BASIC', '2026-11-01 01:00:00', 1),
    (2002, 'QS-C', 'Trần Văn C', '1938-02-10', 'MALE', 'IN_RESIDENCE', '2026-11-01 01:00:00', 'BASIC', '2026-11-01 01:00:00', 1),
    (2003, 'QS-E', 'Lê Thị E', '1942-07-15', 'FEMALE', 'IN_RESIDENCE', '2026-11-23 01:00:00', 'BASIC', '2026-11-23 01:00:00', 1),
    (2004, 'QS-B', 'Phạm Văn B', '1945-03-20', 'MALE', 'IN_RESIDENCE', '2026-11-01 01:00:00', 'BASIC', '2026-11-01 01:00:00', 1),
    (2005, 'QS-G', 'Hoàng Thị G', '1939-09-09', 'FEMALE', 'IN_RESIDENCE', '2026-11-01 01:00:00', 'BASIC', '2026-11-01 01:00:00', 1),
    (2006, 'QS-F', 'Đỗ Văn F', '1941-11-11', 'MALE', 'IN_RESIDENCE', '2026-11-01 01:00:00', 'BASIC', '2026-11-01 01:00:00', 1);
INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value_before, value, starts_at, command,
                            actor_id, by_system)
SELECT 'M01.RESIDENT', id, 'STATUS', 'M01', 'POINT', 'ADMISSION_IN_PROGRESS', 'IN_RESIDENCE', status_since, 'SEED', 1, TRUE
FROM resident WHERE id BETWEEN 2001 AND 2006;

-- Hợp đồng Hiệu lực (004): nội trú dài hạn, B bán trú thứ 2, 4, 6
INSERT INTO contract (id, resident_id, residence_type, start_date, care_level, residency_fee_billing_period, status,
                      signed_date, signing_representative_id, scan_file_id, created_by, created_at, contract_number)
SELECT 3000 + (id - 2000), id, IF(id = 2004, 'DAY_CARE', 'LONG_TERM_RESIDENTIAL'), DATE(status_since), 'BASIC',
       'MONTH', 'ACTIVE', DATE(status_since), 1, 1, 1, status_since, CONCAT('HD-2026-QS', id)
FROM resident WHERE id BETWEEN 2001 AND 2006;
INSERT INTO contract_version (kind, contract_id, services, arrival_schedule, created_at)
SELECT 'DRAFT', id, '[]', IF(residence_type = 'DAY_CARE', '[{"dayOfWeek":1,"date":null,"arrivalTime":"07:30","departureTime":"17:00"},{"dayOfWeek":3,"date":null,"arrivalTime":"07:30","departureTime":"17:00"},{"dayOfWeek":5,"date":null,"arrivalTime":"07:30","departureTime":"17:00"}]', '[]'), created_at FROM contract WHERE id BETWEEN 3001 AND 3006;
INSERT INTO contract_version (kind, contract_id, sequence_no, effective_from, content, residence_type, care_level,
                              created_at, locked_at)
SELECT 'CONTENT', id, 1, start_date,
       JSON_OBJECT('residenceType', residence_type, 'careLevel', 'BASIC', 'startDate', start_date, 'services', JSON_ARRAY(),
                   'arrivalSchedule', IF(residence_type = 'DAY_CARE', CAST('[{"dayOfWeek":1,"date":null,"arrivalTime":"07:30","departureTime":"17:00"},{"dayOfWeek":3,"date":null,"arrivalTime":"07:30","departureTime":"17:00"},{"dayOfWeek":5,"date":null,"arrivalTime":"07:30","departureTime":"17:00"}]' AS JSON), JSON_ARRAY())),
       residence_type, 'BASIC', created_at, created_at
FROM contract WHERE id BETWEEN 3001 AND 3006;

-- Phân bổ giường Đang hiệu lực (003)
INSERT INTO bed_allocation (resident_id, bed_id, status, start_at, checked_care_level, care_level_source) VALUES
    (2001, 1101, 'ACTIVE', '2026-11-01 01:00:00', 'BASIC', 'CURRENT'),
    (2003, 1102, 'ACTIVE', '2026-11-23 01:00:00', 'BASIC', 'CURRENT'),
    (2002, 1103, 'ACTIVE', '2026-11-01 01:00:00', 'BASIC', 'CURRENT'),
    (2006, 1104, 'ACTIVE', '2026-11-01 01:00:00', 'BASIC', 'CURRENT'),
    (2005, 1107, 'ACTIVE', '2026-11-01 01:00:00', 'BASIC', 'CURRENT');

-- Trưởng tầng T của tầng 2 (tầng 3 chưa có Trưởng tầng)
INSERT INTO staff_assignment (kind, staff_id, floor_area_id, starts_at, assigned_by, status)
VALUES ('FLOOR_MANAGER', 9003, 1002, '2026-11-01 00:00:00', 1, 'ACTIVE');

-- Ca đã công bố 25/11 → 31/12: ca ngày, ca đêm tầng 2; ca ngày tầng 3; ca trực Bác sĩ (cơ sở) ban ngày, đêm không có
INSERT INTO catalog_item (id, catalog_type, module, name, status, attributes) VALUES
    (1201, 'M09.SHIFT_TEMPLATE', 'M09', 'Ca ngày QS', 'ACTIVE', JSON_OBJECT('createdAt', '2026-11-01T00:00:00Z', 'startTime', '07:00:00', 'endTime', '18:00:00', 'scopeType', 'FLOOR', 'requiresHandover', CAST('false' AS JSON), 'originalName', 'Ca ngày QS')),
    (1202, 'M09.SHIFT_TEMPLATE', 'M09', 'Ca đêm QS', 'ACTIVE', JSON_OBJECT('createdAt', '2026-11-01T00:00:00Z', 'startTime', '18:00:00', 'endTime', '07:00:00', 'scopeType', 'FLOOR', 'requiresHandover', CAST('false' AS JSON), 'originalName', 'Ca đêm QS')),
    (1203, 'M09.SHIFT_TEMPLATE', 'M09', 'Trực bác sĩ QS', 'ACTIVE', JSON_OBJECT('createdAt', '2026-11-01T00:00:00Z', 'startTime', '07:00:00', 'endTime', '18:00:00', 'scopeType', 'FACILITY', 'requiresHandover', CAST('false' AS JSON), 'originalName', 'Trực bác sĩ QS'));
INSERT INTO shift_schedule (id, month, scope_type, scope_id, status) VALUES
    (1301, '2026-11', 'FLOOR', 1002, 'PUBLISHED'), (1302, '2026-12', 'FLOOR', 1002, 'PUBLISHED'),
    (1303, '2026-11', 'FLOOR', 1003, 'PUBLISHED'), (1304, '2026-12', 'FLOOR', 1003, 'PUBLISHED'),
    (1305, '2026-11', 'FACILITY', NULL, 'PUBLISHED'), (1306, '2026-12', 'FACILITY', NULL, 'PUBLISHED');
INSERT INTO work_shift (schedule_id, template_id, scope_type, scope_id, starts_at, ends_at, requires_handover, status)
WITH RECURSIVE d(day) AS (SELECT DATE('2026-11-25') UNION ALL SELECT day + INTERVAL 1 DAY FROM d WHERE day < '2026-12-31')
SELECT IF(MONTH(day) = 11, 1301, 1302), 1201, 'FLOOR', 1002, TIMESTAMP(day, '00:00:00'), TIMESTAMP(day, '11:00:00'), FALSE, 'PUBLISHED' FROM d
UNION ALL
SELECT IF(MONTH(day) = 11, 1301, 1302), 1202, 'FLOOR', 1002, TIMESTAMP(day, '11:00:00'), TIMESTAMP(day + INTERVAL 1 DAY, '00:00:00'), FALSE, 'PUBLISHED' FROM d
UNION ALL
SELECT IF(MONTH(day) = 11, 1303, 1304), 1201, 'FLOOR', 1003, TIMESTAMP(day, '00:00:00'), TIMESTAMP(day, '11:00:00'), FALSE, 'PUBLISHED' FROM d
UNION ALL
SELECT IF(MONTH(day) = 11, 1305, 1306), 1203, 'FACILITY', NULL, TIMESTAMP(day, '00:00:00'), TIMESTAMP(day, '11:00:00'), FALSE, 'PUBLISHED' FROM d;

-- Thành viên ca: ngày tầng 2 D, D2, P (phụ trách ca), S; đêm tầng 2 Đ (phụ trách ca); ngày tầng 3 D5; trực B
INSERT INTO shift_staff (shift_id, staff_id, on_duty_doctor)
SELECT w.id, m.staff_id, m.doctor FROM work_shift w JOIN (
    SELECT 1201 AS template_id, 1002 AS floor, 9005 AS staff_id, FALSE AS doctor
    UNION ALL SELECT 1201, 1002, 9010, FALSE UNION ALL SELECT 1201, 1002, 9012, FALSE UNION ALL SELECT 1201, 1002, 9006, FALSE
    UNION ALL SELECT 1202, 1002, 9014, FALSE
    UNION ALL SELECT 1201, 1003, 9011, FALSE
    UNION ALL SELECT 1203, NULL, 9004, TRUE) m
    ON m.template_id = w.template_id AND (m.floor <=> w.scope_id)
WHERE w.schedule_id BETWEEN 1301 AND 1306;
INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value_id, starts_at, command, by_system)
SELECT 'M09.SHIFT', w.id, 'LEAD_STAFF', 'M09', 'PERIOD',
       CASE WHEN w.template_id = 1202 THEN 9014 WHEN w.scope_id = 1003 THEN 9011 ELSE 9012 END, w.starts_at, 'SCHEDULED', FALSE
FROM work_shift w WHERE w.schedule_id BETWEEN 1301 AND 1304;

-- Phân công: D phụ trách A, C ca ngày; Đ phụ trách A, C ca đêm; S chăm sóc chính A, C, E ca ngày; D5 phụ trách G
INSERT INTO staff_assignment (shift_id, staff_id, target_type, target_id, role, assignment_kind, assigned_nurse,
                              starts_at, ends_at, status, created_by)
SELECT w.id, a.staff_id, 'RESIDENT', a.resident_id, a.role, 'PRIMARY', a.nurse, w.starts_at, w.ends_at, 'ACTIVE', 1
FROM work_shift w JOIN (
    SELECT 1201 AS template_id, 1002 AS floor, 9005 AS staff_id, 2001 AS resident_id, 'NURSING_STAFF' AS role, TRUE AS nurse
    UNION ALL SELECT 1201, 1002, 9005, 2002, 'NURSING_STAFF', TRUE
    UNION ALL SELECT 1202, 1002, 9014, 2001, 'NURSING_STAFF', TRUE
    UNION ALL SELECT 1202, 1002, 9014, 2002, 'NURSING_STAFF', TRUE
    UNION ALL SELECT 1201, 1002, 9006, 2001, 'CAREGIVER', FALSE
    UNION ALL SELECT 1201, 1002, 9006, 2002, 'CAREGIVER', FALSE
    UNION ALL SELECT 1201, 1002, 9006, 2003, 'CAREGIVER', FALSE
    UNION ALL SELECT 1201, 1003, 9011, 2005, 'NURSING_STAFF', TRUE) a
    ON a.template_id = w.template_id AND a.floor = w.scope_id
WHERE w.schedule_id BETWEEN 1301 AND 1304;

-- Người thân (012): M liên hệ chính của A (xem sức khỏe, nhận khẩn); R đại diện của A (xem chi phí, nhận khẩn); N người thân khác của A;
-- K người thân của E
INSERT INTO family_member (id, full_name, phone_number, created_by, created_at) VALUES
    (4001, 'Người thân M', '0900000001', 1, '2026-11-01 00:00:00'),
    (4002, 'Người đại diện R', '0900000002', 1, '2026-11-01 00:00:00'),
    (4003, 'Người thân N', '0900000003', 1, '2026-11-01 00:00:00'),
    (4004, 'Người thân K', '0900000004', 1, '2026-11-01 00:00:00');
INSERT INTO family_relationship (id, family_member_id, resident_id, relationship, representative, primary_contact,
                                 contact_order, view_health, view_costs, register_visits, receive_urgent, pickup_allowed,
                                 request_services, started_at, created_by, created_at) VALUES
    (4101, 4001, 2001, 'CHILD', FALSE, TRUE, 1, TRUE, FALSE, TRUE, TRUE, FALSE, FALSE, '2026-11-01 00:00:00', 1, '2026-11-01 00:00:00'),
    (4102, 4002, 2001, 'CHILD', TRUE, FALSE, 2, FALSE, TRUE, TRUE, TRUE, FALSE, TRUE, '2026-11-01 00:00:00', 1, '2026-11-01 00:00:00'),
    (4103, 4003, 2001, 'CHILD', FALSE, FALSE, 3, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, '2026-11-01 00:00:00', 1, '2026-11-01 00:00:00'),
    (4104, 4004, 2003, 'CHILD', FALSE, TRUE, 1, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, '2026-11-01 00:00:00', 1, '2026-11-01 00:00:00');
INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value, starts_at, command, actor_id, by_system)
SELECT 'M10.FAMILY_RELATIONSHIP', r.id, x.attr, 'M10', 'POINT', 'true', r.started_at, 'CREATE', 1, FALSE
FROM family_relationship r JOIN (
    SELECT 'REPRESENTATIVE' AS attr UNION ALL SELECT 'PRIMARY_CONTACT' UNION ALL SELECT 'VIEW_HEALTH' UNION ALL SELECT 'VIEW_COSTS'
    UNION ALL SELECT 'REGISTER_VISITS' UNION ALL SELECT 'RECEIVE_URGENT' UNION ALL SELECT 'REQUEST_SERVICES') x
    ON (x.attr = 'REPRESENTATIVE' AND r.representative) OR (x.attr = 'PRIMARY_CONTACT' AND r.primary_contact)
        OR (x.attr = 'VIEW_HEALTH' AND r.view_health) OR (x.attr = 'VIEW_COSTS' AND r.view_costs)
        OR (x.attr = 'REGISTER_VISITS' AND r.register_visits) OR (x.attr = 'RECEIVE_URGENT' AND r.receive_urgent)
        OR (x.attr = 'REQUEST_SERVICES' AND r.request_services)
WHERE r.id BETWEEN 4101 AND 4104;
-- Tài khoản cổng người thân M, R, N, K
INSERT INTO account (username, type, family_member_id, status, password_hash, must_change_password,
                     inactivity_reference_at, created_at, created_by)
SELECT CONCAT('demo.nguoithan.', LOWER(SUBSTRING(full_name, -1))), 'FAMILY_MEMBER', id, 'ACTIVE',
       '$argon2id$v=19$m=16384,t=2,p=1$17mqxm8hOdIjyC60bGYBQw$ja8n7hsdD4zLhrpMS2eSF1MPLbM1/KY00izjSCQkh9c',
       FALSE, '2026-11-01 00:00:00', '2026-11-01 00:00:00', NULL
FROM family_member WHERE id BETWEEN 4001 AND 4004;
INSERT INTO permission_assignment (kind, applies_to, account_id, role, effective_from, actor_id, reason)
SELECT 'ROLE_ASSIGNMENT', 'ACCOUNT', id, 'AC-10', created_at, NULL, 'Dữ liệu nghiệm thu quickstart' FROM account WHERE family_member_id BETWEEN 4001 AND 4004;

-- Tài khoản người cao tuổi A (nội trú) và B (bán trú) với thiết bị đã gắn như 002 DeviceBindings.bind: token thiết bị
-- 'qs007-device-A', 'qs007-device-B', lưu SHA-256 (S1 → S12)
INSERT INTO account (username, type, resident_id, status, password_hash, must_change_password, inactivity_reference_at,
                     created_at, created_by) VALUES
    ('demo.nct.a', 'RESIDENT', 2001, 'ACTIVE', 'x', FALSE, '2026-11-01 00:00:00', '2026-11-01 00:00:00', NULL),
    ('demo.nct.b', 'RESIDENT', 2004, 'ACTIVE', 'x', FALSE, '2026-11-01 00:00:00', '2026-11-01 00:00:00', NULL);
INSERT INTO device_credential (kind, account_id, device_id, secret_hash, created_at)
SELECT 'BOUND_DEVICE', id, IF(resident_id = 2001, '00000000-0000-4000-8000-000000002001', '00000000-0000-4000-8000-000000002004'),
       UNHEX(SHA2(IF(resident_id = 2001, 'qs007-device-A', 'qs007-device-B'), 256)), '2026-11-01 00:00:00'
FROM account WHERE username IN ('demo.nct.a', 'demo.nct.b');

-- A: dị ứng Penicillin (nặng, hồ sơ y tế), nguyện vọng cuối đời Hiệu lực "đưa về nhà", phiếu nguyện vọng khi điều trị
-- (Bệnh viện X, hỏi gia đình trước, báo R); F: nguyện vọng cuối đời "chuyển bệnh viện" (Q55)
INSERT INTO health_item (resident_id, type, free_text, severity_id, source_id, recorded_by, recorded_at)
SELECT 2001, 'ALLERGY', 'Penicillin', s.id, src.id, 4, '2026-11-01 01:00:00'
FROM catalog_item s, catalog_item src
WHERE s.catalog_type = 'M01.ALLERGY_SEVERITY' AND s.id = (SELECT MAX(id) FROM catalog_item WHERE catalog_type = 'M01.ALLERGY_SEVERITY')
  AND src.catalog_type = 'M01.INFORMATION_SOURCE' AND src.id = (SELECT MIN(id) FROM catalog_item WHERE catalog_type = 'M01.INFORMATION_SOURCE');
-- Bản ký của phiếu nguyện vọng (chỉ dòng tệp, không có đối tượng trên S3)
INSERT INTO file (id, uuid, original_name, mime_type, size_bytes, sha256, uploaded_by, uploaded_at, owner_type, owner_id, attached_at)
VALUES (5001, '00000000-0000-4000-8000-000000005001', 'nguyen-vong-A.pdf', 'application/pdf', 1, REPEAT('0', 64), 4,
        '2026-11-01 01:00:00', 'M01.RESIDENT', 2001, '2026-11-01 01:00:00'),
       (5002, '00000000-0000-4000-8000-000000005002', 'nguyen-vong-F.pdf', 'application/pdf', 1, REPEAT('0', 64), 4,
        '2026-11-01 01:00:00', 'M01.RESIDENT', 2006, '2026-11-01 01:00:00');
INSERT INTO wish (kind, resident_id, choice, first_contact, respondent_type, assessing_doctor_id, can_self_determine,
                  assessed_at, support_method, review_date, signer_type, signed_at, signed_copy_file_id, source, status,
                  recorded_by, recorded_at) VALUES
    ('END_OF_LIFE', 2001, 'RETURN_HOME', 'Người thân M', 'RESIDENT', 4, TRUE, '2026-11-01 01:00:00', 'Giải thích trực tiếp',
     '2027-05-01', 'RESIDENT', '2026-11-01 01:00:00', 5001, 'ADMISSION', 'ACTIVE', 4, '2026-11-01 01:00:00'),
    ('END_OF_LIFE', 2006, 'TRANSFER_TO_HOSPITAL', 'Không', 'RESIDENT', 4, TRUE, '2026-11-01 01:00:00', 'Giải thích trực tiếp',
     '2027-05-01', 'RESIDENT', '2026-11-01 01:00:00', 5002, 'ADMISSION', 'ACTIVE', 4, '2026-11-01 01:00:00');
INSERT INTO wish (kind, resident_id, hospital1_name, hospital1_address, ask_family_first, escort, respondent_type, status,
                  recorded_by, recorded_at, notify_family_member_ids) VALUES
    ('TREATMENT', 2001, 'Bệnh viện X', '1 Đường Y, Hà Nội', TRUE, 'FAMILY', 'RESIDENT', 'ACTIVE', 4, '2026-11-01 01:00:00',
     JSON_ARRAY(4002));

-- Nhân viên bếp K trong ca toàn viện (không tầng) để bấm nút hỗ trợ khẩn cấp (H3, H6)
INSERT INTO shift_staff (shift_id, staff_id, on_duty_doctor)
SELECT id, 9007, FALSE FROM work_shift WHERE template_id = 1203 AND schedule_id IN (1305, 1306);

-- D2 phân công cả tầng 2 ca ngày (Q13 giao D2, H5 → H8 D2 tiếp nhận, gọi 115)
INSERT INTO staff_assignment (shift_id, staff_id, target_type, target_id, role, assignment_kind, assigned_nurse, starts_at,
                              ends_at, status, created_by)
SELECT id, 9010, 'FLOOR', 1002, 'NURSING_STAFF', 'PRIMARY', FALSE, starts_at, ends_at, 'ACTIVE', 1
FROM work_shift WHERE template_id = 1201 AND scope_id = 1002 AND schedule_id IN (1301, 1302);
