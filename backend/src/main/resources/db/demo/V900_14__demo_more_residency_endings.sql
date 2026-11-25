-- Dữ liệu demo (chỉ profile demo; E làm 2026-11-26 theo góp ý demo BF-10): thêm bốn người cao tuổi Đang lưu trú để demo
-- trọn luồng Kết thúc lưu trú nhiều lần, giống 9004 Phạm Văn Dũng của V900_13: nội trú dài hạn hết hạn trong CFG-M02-04
-- ngày (Hành chính được nhắc, người đại diện được báo), đã nộp trước và có tiền cọc (demo "Hoàn toàn bộ"), hai món đồ
-- đang gửi, người đại diện có tài khoản app (mật khẩu "Demo2026!").
--   9007 Đặng Văn Hùng   hết hạn sau 5 ngày    người đại diện Đặng Thị Hoa    demo.nguoithan.hoa
--   9008 Bùi Thị Lan     hết hạn sau 12 ngày   người đại diện Bùi Văn Khánh   demo.nguoithan.khanh
--   9009 Đỗ Văn Minh     hết hạn sau 20 ngày   người đại diện Đỗ Thị Ngọc     demo.nguoithan.ngoc
--   9010 Ngô Thị Oanh    hết hạn sau 25 ngày   người đại diện Ngô Văn Phát    demo.nguoithan.phat
-- Giường dùng các giường còn trống sau V900_13 (9005, 9006 phòng nam P102; 9009, 9010 phòng P202). Cách ghi như V900_13.
-- Cú pháp dùng được ở MySQL 8.0 và 8.4.

SET @demo_at = UTC_TIMESTAMP(6);
SET @today = DATE(@demo_at + INTERVAL 7 HOUR);
SET @start_date = DATE_FORMAT(@today - INTERVAL 5 DAY, '%Y-%m-01');
SET @since = TIMESTAMP(@start_date) + INTERVAL 2 HOUR;
SET @office = (SELECT id FROM account WHERE username = 'demo.hanhchinh');
SET @manager = (SELECT id FROM account WHERE username = 'demo.quanly');
SET @accountant = (SELECT id FROM account WHERE username = 'demo.ketoan');
SET @office_staff = (SELECT staff_id FROM account WHERE username = 'demo.hanhchinh');

CREATE TEMPORARY TABLE demo_stay
(
    resident_id    BIGINT PRIMARY KEY,
    profile_code   VARCHAR(32),
    full_name      VARCHAR(100),
    date_of_birth  DATE,
    gender         VARCHAR(10),
    address        VARCHAR(200),
    bed_id         BIGINT,
    room_type_id   BIGINT,
    end_date       DATE,
    payment_code   VARCHAR(16),
    member_id      BIGINT,
    member_name    VARCHAR(100),
    member_phone   VARCHAR(20),
    username       VARCHAR(64),
    prepaid        DECIMAL(15, 0),
    deposit        DECIMAL(15, 0)
);

INSERT INTO demo_stay VALUES
    (9007, 'NCT-2026-009007', 'Đặng Văn Hùng', '1940-02-11', 'MALE', 'Quận Đống Đa, Hà Nội', 9005, 9002,
     @today + INTERVAL 5 DAY, 'NCTDEM9078', 9007, 'Đặng Thị Hoa', '0911000007', 'demo.nguoithan.hoa', 12000000, 5000000),
    (9008, 'NCT-2026-009008', 'Bùi Thị Lan', '1943-07-25', 'FEMALE', 'Quận Tây Hồ, Hà Nội', 9009, 9002,
     @today + INTERVAL 12 DAY, 'NCTDEM9089', 9008, 'Bùi Văn Khánh', '0911000008', 'demo.nguoithan.khanh', 8000000,
     3000000),
    (9009, 'NCT-2026-009009', 'Đỗ Văn Minh', '1937-10-03', 'MALE', 'Quận Hà Đông, Hà Nội', 9006, 9002,
     @today + INTERVAL 20 DAY, 'NCTDEM909A', 9009, 'Đỗ Thị Ngọc', '0911000009', 'demo.nguoithan.ngoc', 15000000, 5000000),
    (9010, 'NCT-2026-009010', 'Ngô Thị Oanh', '1944-04-17', 'FEMALE', 'Quận Nam Từ Liêm, Hà Nội', 9010, 9002,
     @today + INTERVAL 25 DAY, 'NCTDEM9140', 9010, 'Ngô Văn Phát', '0911000010', 'demo.nguoithan.phat', 6000000,
     2000000);

-- 001: hồ sơ, Đang lưu trú, mức Chăm sóc cơ bản
INSERT INTO resident (id, profile_code, full_name, date_of_birth, gender, permanent_address, status, status_since,
                      current_care_level, created_at, created_by)
SELECT resident_id, profile_code, full_name, date_of_birth, gender, address, 'IN_RESIDENCE', @since, 'BASIC',
       @since - INTERVAL 1 DAY, @office
FROM demo_stay;

INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value, starts_at, command, actor_id,
                            by_system, details)
SELECT 'M01.RESIDENT', resident_id, 'STATUS', 'M01', 'POINT', 'ADMISSION_IN_PROGRESS', @since - INTERVAL 1 DAY,
       'CREATE_PROFILE', @office, FALSE, JSON_OBJECT('conditionWaiver', NULL)
FROM demo_stay;

INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value_before, value, starts_at, command,
                            actor_id, by_system)
SELECT 'M01.RESIDENT', resident_id, 'STATUS', 'M01', 'POINT', 'ADMISSION_IN_PROGRESS', 'IN_RESIDENCE', @since,
       'COMPLETE_ADMISSION', @office, FALSE
FROM demo_stay;

INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value, starts_at, actor_id, by_system)
SELECT 'M01.RESIDENT', resident_id, 'CARE_LEVEL', 'M01', 'POINT', 'BASIC', @since, @manager, FALSE
FROM demo_stay;

-- 012: người thân là người đại diện, liên hệ chính, đủ quyền xem
INSERT INTO family_member (id, full_name, phone_number, address, created_by, created_at)
SELECT member_id, member_name, member_phone, address, @office, @since - INTERVAL 1 DAY
FROM demo_stay;

INSERT INTO family_relationship (id, family_member_id, resident_id, relationship, representative, primary_contact,
                                 contact_order, view_health, view_costs, receive_urgent, register_visits,
                                 pickup_allowed, request_services, started_at, created_by, created_at)
SELECT member_id, member_id, resident_id, 'CHILD', TRUE, TRUE, 1, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE,
       @since - INTERVAL 1 DAY, @office, @since - INTERVAL 1 DAY
FROM demo_stay;

INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value, starts_at, command, actor_id,
                            by_system)
SELECT 'M10.FAMILY_RELATIONSHIP', d.member_id, a.attribute, 'M10', 'POINT', 'true', @since - INTERVAL 1 DAY, 'CREATE',
       @office, FALSE
FROM demo_stay d
         JOIN (SELECT 'REPRESENTATIVE' AS attribute
               UNION ALL SELECT 'PRIMARY_CONTACT'
               UNION ALL SELECT 'VIEW_HEALTH'
               UNION ALL SELECT 'VIEW_COSTS'
               UNION ALL SELECT 'RECEIVE_URGENT'
               UNION ALL SELECT 'REGISTER_VISITS'
               UNION ALL SELECT 'PICKUP_ALLOWED'
               UNION ALL SELECT 'REQUEST_SERVICES') a;

-- 004: hợp đồng nội trú dài hạn Hiệu lực, nội dung đủ kỳ tính phí, không yêu cầu cọc (như V900_12, V900_13)
INSERT INTO contract (id, contract_number, resident_id, residence_type, start_date, end_date, current_end_date,
                      care_level, residency_fee_billing_period, status, signed_date, signing_representative_id,
                      scan_file_id, created_by, created_at)
SELECT resident_id, CONCAT('HD-', YEAR(@start_date), '-00', resident_id), resident_id, 'LONG_TERM_RESIDENTIAL',
       @start_date, end_date, end_date, 'BASIC', 'MONTH', 'ACTIVE', @start_date, member_id, 9001, @office, @since
FROM demo_stay;

INSERT INTO contract_version (kind, contract_id, services, arrival_schedule, created_at)
SELECT 'DRAFT', resident_id, '[]', '[]', @since FROM demo_stay;

INSERT INTO contract_version (kind, contract_id, sequence_no, effective_from, content, residence_type, care_level,
                              created_at, locked_at)
SELECT 'CONTENT', resident_id, 1, @start_date,
       JSON_OBJECT('residenceType', 'LONG_TERM_RESIDENTIAL', 'careLevel', 'BASIC', 'startDate', @start_date, 'endDate',
                   end_date, 'roomTypeId', room_type_id, 'services', JSON_ARRAY(), 'arrivalSchedule', JSON_ARRAY(),
                   'residencyFeeBillingPeriod', 'MONTH', 'noDepositRequired', TRUE),
       'LONG_TERM_RESIDENTIAL', 'BASIC', @since, @since
FROM demo_stay;

-- 003: phân bổ giường Đang hiệu lực, giường chuyển Đang sử dụng
INSERT INTO bed_allocation (id, resident_id, bed_id, status, start_at, checked_care_level, care_level_source)
SELECT resident_id, resident_id, bed_id, 'ACTIVE', @since, 'BASIC', 'CURRENT' FROM demo_stay;

UPDATE bed SET state = 'OCCUPIED' WHERE id IN (SELECT bed_id FROM demo_stay);

INSERT INTO bed_state_history (bed_id, from_state, to_state, event, occurred_at, actor_id, basis_type, basis_id,
                               floor_area_id)
SELECT d.bed_id, 'EMPTY', 'OCCUPIED', 'ALLOCATION_STARTED', @since, @office, 'BED_ALLOCATION', d.resident_id,
       r.floor_id
FROM demo_stay d
         JOIN bed b ON b.id = d.bed_id
         JOIN room r ON r.id = b.room_id;

-- 017: sổ số dư, nộp trước và tiền cọc do Kế toán ghi ngày vào ở (bảng tạm chỉ đọc một lần mỗi câu lệnh)
INSERT INTO resident_balance (resident_id, opened_at, opening_contract_id, payment_code, balance, deposit_balance,
                              deposit_status, deposit_status_contract_id)
SELECT resident_id, @since, resident_id, payment_code, prepaid, deposit, 'NOT_REQUIRED', resident_id FROM demo_stay;

INSERT INTO balance_transaction (id, resident_id, ledger, type, amount, occurred_at, method, payer_name, note, status,
                                 confirmed_at, locked_at, created_at, created_by)
SELECT 99000 + (resident_id - 9007) * 2 + 1 + t.n, resident_id, t.ledger, t.type,
       CASE WHEN t.n = 0 THEN prepaid ELSE deposit END, @since + INTERVAL 1 HOUR, 'CASH', member_name,
       CASE WHEN t.n = 0 THEN 'Nộp trước tiền lưu trú' END, 'CONFIRMED', @since + INTERVAL 1 HOUR,
       @since + INTERVAL 1 HOUR, @since + INTERVAL 1 HOUR, @accountant
FROM demo_stay
         JOIN (SELECT 0 AS n, 'BALANCE' AS ledger, 'CASH_PAYMENT' AS type
               UNION ALL SELECT 1, 'DEPOSIT', 'DEPOSIT_RECEIPT') t;

-- 013: hai món đồ đang gửi mỗi người (không dùng loại có giá trị vì bắt buộc có ảnh)
SET @good = (SELECT id FROM catalog_item WHERE catalog_type = 'M12.CONDITION_LEVEL' AND code = 'GOOD');
SET @used = (SELECT id FROM catalog_item WHERE catalog_type = 'M12.CONDITION_LEVEL' AND code = 'SIGNS_OF_USE');

CREATE TEMPORARY TABLE demo_belonging
(
    id           BIGINT PRIMARY KEY,
    resident_id  BIGINT,
    type_code    VARCHAR(32),
    description  VARCHAR(200),
    quantity     DECIMAL(10, 2),
    unit         VARCHAR(20),
    condition_id BIGINT,
    giver_id     BIGINT
);

INSERT INTO demo_belonging VALUES
    (99001, 9007, 'WHEELCHAIR', 'Xe lăn tay đẩy màu đen', 1, 'chiếc', @used, 9007),
    (99002, 9007, 'CLOTHING', 'Quần áo thu đông', 4, 'bộ', @good, 9007),
    (99003, 9008, 'GLASSES', 'Kính đọc sách gọng bạc', 1, 'cái', @good, 9008),
    (99004, 9008, 'DOCUMENTS', 'Sổ khám bệnh, thẻ bảo hiểm y tế', 2, 'cuốn', @good, 9008),
    (99005, 9009, 'PERSONAL_ITEMS', 'Máy đo huyết áp cá nhân', 1, 'cái', @used, 9009),
    (99006, 9009, 'CLOTHING', 'Chăn len, gối', 2, 'cái', @used, 9009),
    (99007, 9010, 'GLASSES', 'Kính lão gọng đỏ', 1, 'cái', @good, 9010),
    (99008, 9010, 'PERSONAL_ITEMS', 'Bộ đồ dùng vệ sinh cá nhân', 1, 'bộ', @good, 9010);

INSERT INTO belonging (id, resident_id, type_id, description, quantity, unit, intake_handover_id, status, holder_type,
                       holder_id, storage_location_id, condition_level_id, current_handover_id, current_handover_at,
                       version, created_by, created_at)
SELECT d.id, d.resident_id, t.id, d.description, d.quantity, d.unit, d.id, 'HELD', 'STAFF', @office_staff, 9703,
       d.condition_id, d.id, @since, 1, @office, @since
FROM demo_belonging d
         JOIN catalog_item t ON t.catalog_type = 'M12.BELONGING_TYPE' AND t.code = d.type_code;

INSERT INTO belonging_handover (id, belonging_id, resident_id, kind, target_status, giver_type, giver_id,
                                recipient_type, recipient_id, occurred_at, quantity, condition_level_id,
                                storage_location_id, holder_absent, valuable, photo_file_ids, recorded_by, recorded_at)
SELECT id, id, resident_id, 'INTAKE', 'HELD', 'FAMILY_MEMBER', giver_id, 'STAFF', @office_staff, @since, quantity,
       condition_id, 9703, FALSE, FALSE, JSON_ARRAY(), @office, @since
FROM demo_belonging;

DROP TEMPORARY TABLE demo_belonging;

-- 002: tài khoản người thân, vai trò Người thân (AC-10); trạng thái, vai trò ghi vào bảng chung (như V900_13)
INSERT INTO account (username, type, family_member_id, status, password_hash, must_change_password,
                     inactivity_reference_at, created_at, created_by)
SELECT username, 'FAMILY_MEMBER', member_id, 'ACTIVE',
       '$argon2id$v=19$m=16384,t=2,p=1$17mqxm8hOdIjyC60bGYBQw$ja8n7hsdD4zLhrpMS2eSF1MPLbM1/KY00izjSCQkh9c',
       FALSE, @demo_at - INTERVAL 1 DAY, @demo_at - INTERVAL 1 DAY, @office
FROM demo_stay;

DROP TEMPORARY TABLE demo_stay;

INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value, starts_at, actor_id, by_system,
                            details, unique_key)
SELECT 'M15.ACCOUNT', id, 'STATUS', 'M15', 'POINT', 'ACTIVE', created_at, @office, FALSE,
       JSON_OBJECT('lockCause', NULL, 'auditLogId', NULL),
       CONCAT('M15.ASH:', id, ':', DATE_FORMAT(created_at, '%Y-%m-%dT%H:%i:%s.%f'))
FROM account
WHERE username IN ('demo.nguoithan.hoa', 'demo.nguoithan.khanh', 'demo.nguoithan.ngoc', 'demo.nguoithan.phat');

INSERT INTO permission_assignment (kind, applies_to, account_id, role, effective_from, actor_id, reason)
SELECT 'ROLE_ASSIGNMENT', 'ACCOUNT', id, 'AC-10', created_at, @office, 'Dữ liệu mẫu demo người thân (góp ý demo BF-10)'
FROM account
WHERE username IN ('demo.nguoithan.hoa', 'demo.nguoithan.khanh', 'demo.nguoithan.ngoc', 'demo.nguoithan.phat');
