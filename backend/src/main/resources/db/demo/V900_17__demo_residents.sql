-- Dữ liệu demo (chỉ profile demo; chủ dự án duyệt 2026-11-25: dữ liệu demo đầy đủ để chạy hết quy trình của viện):
-- thêm người cao tuổi Đang lưu trú ở cả năm tầng, đủ ba mức chăm sóc, để các luồng có nhiều người (lịch ca, phân công,
-- checklist, cảnh báo, chi phí, số dư, người thân). Ghi như V900_01, V900_02, V900_12 (lệnh của ứng dụng):
--   1. tám hồ sơ 9101 → 9108 Đang lưu trú từ 30 ngày trước (dòng lịch sử CREATE_PROFILE, COMPLETE_ADMISSION, CARE_LEVEL);
--   2. mỗi người một người thân là người đại diện kiêm liên hệ chính, đủ quyền (012); B (9002) của V900_02 chưa có người
--      thân nào nên thêm người đại diện cho B;
--   3. hợp đồng nội trú dài hạn Hiệu lực, dịch vụ chăm sóc theo mức và phí lưu trú theo loại phòng (trong gói), đơn giá
--      ghi lúc ký, phiên bản nội dung 0 dạng ContractSnapshot, không yêu cầu cọc (004);
--   4. phân bổ giường Đang hiệu lực, giường Đang sử dụng (003; mức phòng dùng FREQUENT cho mức REGULAR của người cao tuổi);
--   5. sổ số dư với mã nộp tiền đúng ký tự kiểm tra của PaymentCode (017), số dư 0 như V900_12;
--   6. phân công Điều dưỡng phụ trách và Nhân viên chăm sóc của tầng trong mọi ca từ ngày nạp (008), cho người mới, cho
--      B (V900_09, V900_11 không phân công B) và cho A từ ngày thứ 8 (V900_02, V900_11 có 7 ngày).
-- Không gồm: bản đồng ý chia sẻ dữ liệu, đánh giá, kế hoạch chăm sóc, đơn thuốc (tạo bằng ứng dụng). Cú pháp dùng được ở
-- MySQL 8.0 và 8.4.

SET @demo_at = UTC_TIMESTAMP(6);
SET @since = @demo_at - INTERVAL 30 DAY;
SET @start_date = DATE(@since + INTERVAL 7 HOUR);
SET @office = (SELECT id FROM account WHERE username = 'demo.hanhchinh');
SET @manager = (SELECT id FROM account WHERE username = 'demo.quanly');

CREATE TEMPORARY TABLE demo_resident (
    id BIGINT PRIMARY KEY, full_name VARCHAR(100), birth DATE, gender VARCHAR(8), address VARCHAR(200),
    care_level VARCHAR(8), bed_id BIGINT, family_name VARCHAR(100), family_phone VARCHAR(12),
    relationship VARCHAR(12), payment_code VARCHAR(16));
INSERT INTO demo_resident VALUES
    (9101, 'Lê Thị Cúc', '1938-03-12', 'FEMALE', 'Quận Hoàng Mai, Hà Nội', 'BASIC', 9002,
     'Lê Văn Cường', '0911000101', 'CHILD', 'NCTDEMB01F'),
    (9102, 'Phạm Văn Đức', '1942-07-08', 'MALE', 'Quận Thanh Xuân, Hà Nội', 'REGULAR', 9003,
     'Phạm Thị Duyên', '0911000102', 'CHILD', 'NCTDEMB02G'),
    (9103, 'Hoàng Văn Em', '1945-01-25', 'MALE', 'Huyện Gia Lâm, Hà Nội', 'BASIC', 9004,
     'Hoàng Thị Én', '0911000103', 'SPOUSE', 'NCTDEMB03H'),
    (9104, 'Đỗ Thị Gấm', '1936-10-30', 'FEMALE', 'Quận Long Biên, Hà Nội', 'REGULAR', 9009,
     'Đỗ Văn Giang', '0911000104', 'CHILD', 'NCTDEMB04J'),
    (9105, 'Vũ Văn Hải', '1940-05-17', 'MALE', 'Quận Tây Hồ, Hà Nội', 'SPECIAL', 9101,
     'Vũ Thị Hằng', '0911000105', 'CHILD', 'NCTDEMB05K'),
    (9106, 'Ngô Thị Kim', '1943-12-02', 'FEMALE', 'Quận Hà Đông, Hà Nội', 'BASIC', 9111,
     'Ngô Văn Khoa', '0911000106', 'GRANDCHILD', 'NCTDEMB06M'),
    (9107, 'Bùi Văn Long', '1939-08-21', 'MALE', 'Huyện Đông Anh, Hà Nội', 'REGULAR', 9125,
     'Bùi Thị Loan', '0911000107', 'CHILD', 'NCTDEMB07N'),
    (9108, 'Trịnh Thị Mận', '1941-04-09', 'FEMALE', 'Quận Bắc Từ Liêm, Hà Nội', 'SPECIAL', 9129,
     'Trịnh Văn Minh', '0911000108', 'SIBLING', 'NCTDEMB08P');

-- 001: hồ sơ Đang lưu trú
INSERT INTO resident (id, profile_code, full_name, date_of_birth, gender, permanent_address, status, status_since,
                      current_care_level, created_at, created_by)
SELECT r.id, CONCAT('NCT-', YEAR(@start_date), '-00', r.id), r.full_name, r.birth, r.gender, r.address,
       'IN_RESIDENCE', @since, r.care_level, @since - INTERVAL 3 DAY, @office
FROM demo_resident r;

INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value, starts_at, command, actor_id,
                            by_system, details)
SELECT 'M01.RESIDENT', r.id, 'STATUS', 'M01', 'POINT', 'ADMISSION_IN_PROGRESS', @since - INTERVAL 3 DAY,
       'CREATE_PROFILE', @office, FALSE, JSON_OBJECT('conditionWaiver', NULL)
FROM demo_resident r;

INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value_before, value, starts_at, command,
                            actor_id, by_system)
SELECT 'M01.RESIDENT', r.id, 'STATUS', 'M01', 'POINT', 'ADMISSION_IN_PROGRESS', 'IN_RESIDENCE', @since,
       'COMPLETE_ADMISSION', @office, FALSE
FROM demo_resident r;

INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value, starts_at, actor_id, by_system)
SELECT 'M01.RESIDENT', r.id, 'CARE_LEVEL', 'M01', 'POINT', r.care_level, @since, @manager, FALSE
FROM demo_resident r;

-- 012: người đại diện kiêm liên hệ chính (người mới và B 9002)
CREATE TEMPORARY TABLE demo_family (
    id BIGINT PRIMARY KEY, resident_id BIGINT, full_name VARCHAR(100), phone VARCHAR(12), address VARCHAR(200),
    relationship VARCHAR(12));
INSERT INTO demo_family
SELECT r.id, r.id, r.family_name, r.family_phone, r.address, r.relationship FROM demo_resident r;
INSERT INTO demo_family VALUES
    (9109, 9002, 'Trần Thị Bích', '0911000109', 'Quận Đống Đa, Hà Nội', 'CHILD');

INSERT INTO family_member (id, full_name, phone_number, address, created_by, created_at)
SELECT f.id, f.full_name, f.phone, f.address, @office, @since - INTERVAL 3 DAY FROM demo_family f;

INSERT INTO family_relationship (id, family_member_id, resident_id, relationship, representative, primary_contact,
                                 contact_order, view_health, view_costs, receive_urgent, register_visits,
                                 pickup_allowed, request_services, started_at, created_by, created_at)
SELECT f.id, f.id, f.resident_id, f.relationship, TRUE, TRUE, 1, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE,
       @since - INTERVAL 3 DAY, @office, @since - INTERVAL 3 DAY
FROM demo_family f;

INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value, starts_at, command, actor_id,
                            by_system)
SELECT 'M10.FAMILY_RELATIONSHIP', f.id, a.attribute, 'M10', 'POINT', 'true', @since - INTERVAL 3 DAY, 'CREATE',
       @office, FALSE
FROM demo_family f
         JOIN (SELECT 'REPRESENTATIVE' AS attribute UNION ALL SELECT 'PRIMARY_CONTACT' UNION ALL SELECT 'VIEW_HEALTH'
               UNION ALL SELECT 'VIEW_COSTS' UNION ALL SELECT 'RECEIVE_URGENT' UNION ALL SELECT 'REGISTER_VISITS'
               UNION ALL SELECT 'PICKUP_ALLOWED' UNION ALL SELECT 'REQUEST_SERVICES') a;

DROP TEMPORARY TABLE demo_family;

-- 004: hợp đồng; dịch vụ chăm sóc theo mức (V900_05, V900_06), phí lưu trú theo loại phòng (V900_05, V900_14)
CREATE TEMPORARY TABLE demo_contract AS
SELECT r.id AS resident_id, r.id AS contract_id, r.care_level, r.bed_id, rm.room_type_id,
       (SELECT s.id FROM catalog_item s
        WHERE s.catalog_type = 'M02.SERVICE' AND s.status = 'ACTIVE' AND s.attributes ->> '$.type' = 'CARE'
          AND s.attributes ->> '$.careLevelCode' = r.care_level ORDER BY s.id LIMIT 1) AS care_service_id,
       (SELECT s.id FROM catalog_item s
        WHERE s.catalog_type = 'M02.SERVICE' AND s.status = 'ACTIVE' AND s.attributes ->> '$.type' = 'RESIDENCY_FEE'
          AND CAST(s.attributes ->> '$.roomTypeId' AS UNSIGNED) = rm.room_type_id ORDER BY s.id LIMIT 1)
           AS fee_service_id
FROM demo_resident r JOIN bed b ON b.id = r.bed_id JOIN room rm ON rm.id = b.room_id;

ALTER TABLE demo_contract ADD COLUMN care_price DECIMAL(15, 0), ADD COLUMN fee_price DECIMAL(15, 0);
UPDATE demo_contract c
SET c.care_price = (SELECT p.unit_price FROM price_version p WHERE p.service_id = c.care_service_id
                    AND p.effective_from <= @start_date ORDER BY p.effective_from DESC LIMIT 1),
    c.fee_price = (SELECT p.unit_price FROM price_version p WHERE p.service_id = c.fee_service_id
                   AND p.effective_from <= @start_date ORDER BY p.effective_from DESC LIMIT 1);

INSERT INTO contract (id, contract_number, resident_id, residence_type, start_date, end_date, care_level, room_type_id,
                      residency_fee_billing_period, no_deposit_required, status, signed_date,
                      signing_representative_id, scan_file_id, created_by, created_at)
SELECT c.contract_id, CONCAT('HD-', YEAR(@start_date), '-00', c.contract_id), c.resident_id, 'LONG_TERM_RESIDENTIAL',
       @start_date, NULL, c.care_level, c.room_type_id, 'MONTH', TRUE, 'ACTIVE', @start_date, c.resident_id, 9001,
       @office, @since - INTERVAL 2 DAY
FROM demo_contract c;

INSERT INTO contract_version (kind, contract_id, services, arrival_schedule, recorded_prices, created_at, locked_at)
SELECT 'DRAFT', c.contract_id,
       JSON_ARRAY(JSON_OBJECT('serviceId', c.care_service_id, 'inPackage', TRUE, 'quantity', 1,
                              'customUnitPrice', NULL),
                  JSON_OBJECT('serviceId', c.fee_service_id, 'inPackage', TRUE, 'quantity', 1,
                              'customUnitPrice', NULL)),
       JSON_ARRAY(),
       JSON_OBJECT(CAST(c.care_service_id AS CHAR), c.care_price, CAST(c.fee_service_id AS CHAR), c.fee_price),
       @since - INTERVAL 2 DAY, @since
FROM demo_contract c;

INSERT INTO contract_version (kind, contract_id, sequence_no, effective_from, content, residence_type, care_level,
                              room_type_id, created_at, locked_at)
SELECT 'CONTENT', c.contract_id, 0, @start_date,
       JSON_OBJECT('residentId', c.resident_id, 'residenceType', 'LONG_TERM_RESIDENTIAL',
                   'startDate', DATE_FORMAT(@start_date, '%Y-%m-%d'), 'endDate', NULL, 'careLevel', c.care_level,
                   'roomTypeId', c.room_type_id, 'bedId', NULL, 'residencyFeeBillingPeriod', 'MONTH',
                   'services', JSON_ARRAY(
                           JSON_OBJECT('serviceId', LEAST(c.care_service_id, c.fee_service_id), 'inPackage', TRUE,
                                       'quantity', 1, 'customUnitPrice', NULL,
                                       'recordedUnitPrice', IF(c.care_service_id < c.fee_service_id, c.care_price,
                                                               c.fee_price)),
                           JSON_OBJECT('serviceId', GREATEST(c.care_service_id, c.fee_service_id), 'inPackage', TRUE,
                                       'quantity', 1, 'customUnitPrice', NULL,
                                       'recordedUnitPrice', IF(c.care_service_id < c.fee_service_id, c.fee_price,
                                                               c.care_price))),
                   'noDepositRequired', TRUE, 'depositAmount', NULL, 'bedHoldPolicy', NULL,
                   'arrivalSchedule', JSON_ARRAY(), 'terminationCondition', NULL),
       'LONG_TERM_RESIDENTIAL', c.care_level, c.room_type_id, @since, @since
FROM demo_contract c;

-- 003: phân bổ giường Đang hiệu lực, giường Đang sử dụng
INSERT INTO bed_allocation (id, resident_id, bed_id, status, start_at, checked_care_level, care_level_source)
SELECT c.resident_id, c.resident_id, c.bed_id, 'ACTIVE', @since, IF(c.care_level = 'REGULAR', 'FREQUENT', c.care_level),
       'CURRENT'
FROM demo_contract c;

UPDATE bed b JOIN demo_contract c ON c.bed_id = b.id SET b.state = 'OCCUPIED';

INSERT INTO bed_state_history (bed_id, from_state, to_state, event, occurred_at, actor_id, basis_type, basis_id,
                               floor_area_id)
SELECT a.bed_id, 'EMPTY', 'OCCUPIED', 'ALLOCATION_STARTED', @since, @office, 'BED_ALLOCATION', a.id, r.floor_id
FROM bed_allocation a JOIN bed b ON b.id = a.bed_id JOIN room r ON r.id = b.room_id
WHERE a.id BETWEEN 9101 AND 9108;

-- 017: sổ số dư
INSERT INTO resident_balance (resident_id, opened_at, opening_contract_id, payment_code, deposit_status,
                              deposit_status_contract_id)
SELECT r.id, @since - INTERVAL 2 DAY, r.id, r.payment_code, 'NOT_REQUIRED', r.id FROM demo_resident r;

DROP TEMPORARY TABLE demo_contract;

-- 008: phân công trong mọi ca từ ngày nạp; Điều dưỡng phụ trách là người phụ trách ca, Nhân viên chăm sóc là thành
-- viên còn lại của ca (không phải Bác sĩ trực)
CREATE TEMPORARY TABLE demo_target AS
SELECT r.id AS resident_id, rm.floor_id
FROM demo_resident r JOIN bed b ON b.id = r.bed_id JOIN room rm ON rm.id = b.room_id;
INSERT INTO demo_target VALUES (9002, 9002), (9001, 9001);

CREATE TEMPORARY TABLE demo_target_shift AS
SELECT t.resident_id, w.id AS shift_id, w.starts_at, w.ends_at, w.lead_staff_id,
       (SELECT m.staff_id FROM shift_staff m
        WHERE m.shift_id = w.id AND m.staff_id <> w.lead_staff_id AND NOT m.on_duty_doctor ORDER BY m.staff_id LIMIT 1)
           AS caregiver_id
FROM demo_target t
         JOIN work_shift w ON w.scope_type = 'FLOOR' AND w.scope_id = t.floor_id AND w.status = 'PUBLISHED'
WHERE w.starts_at >= TIMESTAMP(DATE(@demo_at + INTERVAL 7 HOUR))
  AND NOT EXISTS (SELECT 1 FROM staff_assignment x
                  WHERE x.shift_id = w.id AND x.target_type = 'RESIDENT' AND x.target_id = t.resident_id);

INSERT INTO staff_assignment (shift_id, staff_id, target_type, target_id, role, assignment_kind, assigned_nurse,
                              starts_at, ends_at, status, created_by)
SELECT s.shift_id, s.lead_staff_id, 'RESIDENT', s.resident_id, 'NURSING_STAFF', 'PRIMARY', TRUE, s.starts_at,
       s.ends_at, IF(s.starts_at <= @demo_at, 'ACTIVE', 'SCHEDULED'), @manager
FROM demo_target_shift s;

INSERT INTO staff_assignment (shift_id, staff_id, target_type, target_id, role, assignment_kind, assigned_nurse,
                              starts_at, ends_at, status, created_by)
SELECT s.shift_id, s.caregiver_id, 'RESIDENT', s.resident_id, 'CAREGIVER', 'PRIMARY', FALSE, s.starts_at, s.ends_at,
       IF(s.starts_at <= @demo_at, 'ACTIVE', 'SCHEDULED'), @manager
FROM demo_target_shift s
WHERE s.caregiver_id IS NOT NULL;

DROP TEMPORARY TABLE demo_target_shift;
DROP TEMPORARY TABLE demo_target;
DROP TEMPORARY TABLE demo_resident;
