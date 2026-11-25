-- Dữ liệu demo (chỉ profile demo; E làm 2026-11-25 theo góp ý demo BF-10 của nhóm): bốn người cao tuổi Đang lưu trú có
-- hợp đồng ở các mốc của luồng nhắc hợp đồng (004 FR-026, FR-029) và tài khoản người thân để demo app người thân.
--   9003 Lê Thị Cúc      nội trú dài hạn, hết hạn sau đúng 30 ngày (ngày đầu khoảng CFG-M02-04: Hành chính được nhắc, người
--                        đại diện được báo một lần)
--   9004 Phạm Văn Dũng   nội trú dài hạn, hết hạn sau 10 ngày (nhắc Hành chính mỗi ngày)
--   9005 Hoàng Thị Em    nội trú dài hạn, đã hết hạn 3 ngày trước (job hằng ngày đánh dấu quá hạn, nhắc Hành chính, báo
--                        người đại diện ngày đầu quá hạn)
--   9006 Võ Văn Phúc     nội trú ngắn ngày, hết hạn hôm nay (nhắc tạo Kết thúc lưu trú hoặc điều chỉnh hợp đồng)
-- Mỗi người có giường, hợp đồng Hiệu lực (nội dung đủ kỳ tính phí, không cọc như V900_12), sổ số dư, một người thân là
-- người đại diện (xem chi phí, nhận thông báo) có tài khoản; 9003, 9004, 9006 đã nộp trước (9004 có thêm tiền cọc),
-- 9004, 9005, 9006 có đồ đang gửi. Thêm tài khoản cho hai người thân của người cao tuổi 9001.
-- Ngày tính theo ngày nạp (giờ Việt Nam), nên nạp vào ngày nào cũng ra đúng các mốc trên. Ghi như lệnh của ứng dụng ghi
-- (dòng lịch sử trạng thái, lịch sử giường) theo mẫu V900_02. Mật khẩu tài khoản người thân: "Demo2026!" (như V002_90).
-- Cú pháp dùng được ở MySQL 8.0 và 8.4.

SET @demo_at = UTC_TIMESTAMP(6);
SET @today = DATE(@demo_at + INTERVAL 7 HOUR);
-- Vào ở từ ngày 1 của tháng (lùi 5 ngày khi nạp đầu tháng, để ngày kết thúc 3 ngày trước vẫn sau ngày vào): kỳ chi phí
-- trước đó không có ngày lưu trú, bảng kỳ cuối là bảng duy nhất cần chốt khi demo Kết thúc lưu trú. 9:00 giờ Việt Nam.
SET @start_date = DATE_FORMAT(@today - INTERVAL 5 DAY, '%Y-%m-01');
SET @since = TIMESTAMP(@start_date) + INTERVAL 2 HOUR;
SET @office = (SELECT id FROM account WHERE username = 'demo.hanhchinh');
SET @manager = (SELECT id FROM account WHERE username = 'demo.quanly');

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
    residence_type VARCHAR(40),
    end_date       DATE,
    payment_code   VARCHAR(16),
    member_id      BIGINT,
    member_name    VARCHAR(100),
    member_phone   VARCHAR(20),
    username       VARCHAR(64)
);

INSERT INTO demo_stay VALUES
    (9003, 'NCT-2026-009003', 'Lê Thị Cúc', '1942-03-14', 'FEMALE', 'Quận Hai Bà Trưng, Hà Nội', 9002, 9001,
     'LONG_TERM_RESIDENTIAL', @today + INTERVAL 30 DAY, 'NCTDEM9034', 9003, 'Lê Văn Cường', '0911000003',
     'demo.nguoithan.cuong'),
    (9004, 'NCT-2026-009004', 'Phạm Văn Dũng', '1938-08-01', 'MALE', 'Quận Thanh Xuân, Hà Nội', 9003, 9002,
     'LONG_TERM_RESIDENTIAL', @today + INTERVAL 10 DAY, 'NCTDEM9045', 9004, 'Phạm Thị Dịu', '0911000004',
     'demo.nguoithan.diu'),
    (9005, 'NCT-2026-009005', 'Hoàng Thị Em', '1945-12-20', 'FEMALE', 'Quận Long Biên, Hà Nội', 9008, 9001,
     'LONG_TERM_RESIDENTIAL', @today - INTERVAL 3 DAY, 'NCTDEM9056', 9005, 'Hoàng Văn Hải', '0911000005',
     'demo.nguoithan.hai'),
    (9006, 'NCT-2026-009006', 'Võ Văn Phúc', '1941-06-09', 'MALE', 'Quận Hoàng Mai, Hà Nội', 9004, 9002,
     'SHORT_TERM_RESIDENTIAL', @today, 'NCTDEM9067', 9006, 'Võ Thị Phương', '0911000006', 'demo.nguoithan.phuong');

-- 001: hồ sơ, Đang lưu trú, mức Chăm sóc cơ bản (dòng lịch sử như lệnh Tạo hồ sơ, Hoàn tất tiếp nhận)
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

-- 012: người thân là người đại diện, liên hệ chính, xem chi phí, xem sức khỏe, nhận thông báo khẩn
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

-- 004: hợp đồng Hiệu lực với ngày kết thúc theo từng mốc; nội dung đủ kỳ tính phí, không cọc (như V900_12)
INSERT INTO contract (id, contract_number, resident_id, residence_type, start_date, end_date, current_end_date,
                      care_level, residency_fee_billing_period, status, signed_date, signing_representative_id,
                      scan_file_id, created_by, created_at)
SELECT resident_id, CONCAT('HD-', YEAR(@start_date), '-00', resident_id), resident_id, residence_type, @start_date,
       end_date, end_date, 'BASIC', 'MONTH', 'ACTIVE', @start_date, member_id, 9001, @office, @since
FROM demo_stay;

INSERT INTO contract_version (kind, contract_id, services, arrival_schedule, created_at)
SELECT 'DRAFT', resident_id, '[]', '[]', @since FROM demo_stay;

INSERT INTO contract_version (kind, contract_id, sequence_no, effective_from, content, residence_type, care_level,
                              created_at, locked_at)
SELECT 'CONTENT', resident_id, 1, @start_date,
       JSON_OBJECT('residenceType', residence_type, 'careLevel', 'BASIC', 'startDate', @start_date, 'endDate',
                   end_date, 'roomTypeId', room_type_id, 'services', JSON_ARRAY(), 'arrivalSchedule', JSON_ARRAY(),
                   'residencyFeeBillingPeriod', 'MONTH', 'noDepositRequired', TRUE),
       residence_type, 'BASIC', @since, @since
FROM demo_stay;

-- 003: phân bổ giường Đang hiệu lực từ ngày vào ở, giường chuyển Đang sử dụng
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

-- 017: sổ số dư (số dư, cọc bằng 0; nộp tiền ghi bằng ứng dụng), mã nộp tiền đúng ký tự kiểm tra của PaymentCode
INSERT INTO resident_balance (resident_id, opened_at, opening_contract_id, payment_code, deposit_status,
                              deposit_status_contract_id)
SELECT resident_id, @since, resident_id, payment_code, 'NOT_REQUIRED', resident_id FROM demo_stay;

-- 017: nộp trước và tiền cọc do Kế toán ghi ngày vào ở (như lệnh Ghi nộp tiền mặt), để demo hoàn số dư, hoàn cọc
-- ("Hoàn toàn bộ") khi kết thúc lưu trú; 9005 không nộp trước, bảng kỳ cuối thành khoản phải nộp (người thân nộp trực
-- tiếp cho Kế toán)
SET @accountant = (SELECT id FROM account WHERE username = 'demo.ketoan');

CREATE TEMPORARY TABLE demo_payment
(
    id          BIGINT PRIMARY KEY,
    resident_id BIGINT,
    ledger      VARCHAR(10),
    type        VARCHAR(20),
    amount      DECIMAL(15, 0),
    payer_name  VARCHAR(100),
    note        VARCHAR(200)
);

INSERT INTO demo_payment VALUES
    (9001, 9003, 'BALANCE', 'CASH_PAYMENT', 15000000, 'Lê Văn Cường', 'Nộp trước tiền lưu trú'),
    (9002, 9004, 'BALANCE', 'CASH_PAYMENT', 10000000, 'Phạm Thị Dịu', 'Nộp trước tiền lưu trú'),
    (9003, 9004, 'DEPOSIT', 'DEPOSIT_RECEIPT', 5000000, 'Phạm Thị Dịu', NULL),
    (9004, 9006, 'BALANCE', 'CASH_PAYMENT', 3000000, 'Võ Thị Phương', 'Nộp trước tiền lưu trú ngắn ngày');

INSERT INTO balance_transaction (id, resident_id, ledger, type, amount, occurred_at, method, payer_name, note, status,
                                 confirmed_at, locked_at, created_at, created_by)
SELECT id, resident_id, ledger, type, amount, @since + INTERVAL 1 HOUR, 'CASH', payer_name, note, 'CONFIRMED',
       @since + INTERVAL 1 HOUR, @since + INTERVAL 1 HOUR, @since + INTERVAL 1 HOUR, @accountant
FROM demo_payment;

-- Bảng tạm chỉ đọc được một lần trong một câu lệnh (MySQL): cộng theo người trong một bảng dẫn xuất
UPDATE resident_balance b
    JOIN (SELECT resident_id,
                 SUM(CASE WHEN ledger = 'BALANCE' THEN amount ELSE 0 END) AS balance,
                 SUM(CASE WHEN ledger = 'DEPOSIT' THEN amount ELSE 0 END) AS deposit
          FROM demo_payment
          GROUP BY resident_id) p ON p.resident_id = b.resident_id
SET b.balance         = p.balance,
    b.deposit_balance = p.deposit;

DROP TEMPORARY TABLE demo_payment;

-- 013: đồ đang gửi (Hành chính nhận từ người đại diện ngày vào ở, như lệnh Tiếp nhận), để demo trả đồ khi kết thúc lưu
-- trú; không dùng loại đồ có giá trị (bắt buộc có ảnh). Mã loại, mức tình trạng tra theo mã danh mục của V013_01.
SET @office_staff = (SELECT staff_id FROM account WHERE username = 'demo.hanhchinh');
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
    (9001, 9005, 'GLASSES', 'Kính lão gọng nâu', 1, 'cái', @good, 9005),
    (9002, 9005, 'CLOTHING', 'Quần áo mùa đông', 5, 'bộ', @used, 9005),
    (9003, 9004, 'WHEELCHAIR', 'Xe lăn gấp gọn màu xanh', 1, 'chiếc', @used, 9004),
    (9004, 9004, 'DOCUMENTS', 'Sổ khám bệnh, thẻ bảo hiểm y tế', 2, 'cuốn', @good, 9004),
    (9005, 9006, 'PERSONAL_ITEMS', 'Bộ đồ dùng vệ sinh cá nhân', 1, 'bộ', @good, 9006);

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

-- 002: tài khoản người thân (Rạng là người đại diện của 9001, Mơ xem chi phí của 9001, bốn người đại diện mới), vai
-- trò Người thân (AC-10)
INSERT INTO account (username, type, family_member_id, status, password_hash, must_change_password,
                     inactivity_reference_at, created_at, created_by)
SELECT a.username, 'FAMILY_MEMBER', a.member_id, 'ACTIVE',
       '$argon2id$v=19$m=16384,t=2,p=1$17mqxm8hOdIjyC60bGYBQw$ja8n7hsdD4zLhrpMS2eSF1MPLbM1/KY00izjSCQkh9c',
       FALSE, @demo_at - INTERVAL 1 DAY, @demo_at - INTERVAL 1 DAY, @office
FROM (SELECT 'demo.nguoithan.rang' AS username, 9001 AS member_id
      UNION ALL SELECT 'demo.nguoithan.mo', 9002
      UNION ALL SELECT username, member_id FROM demo_stay) a;

-- Trạng thái, vai trò ghi vào bảng chung từ V002_95 (như AccountStore.history, addRole)
INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value, starts_at, actor_id, by_system,
                            details, unique_key)
SELECT 'M15.ACCOUNT', id, 'STATUS', 'M15', 'POINT', 'ACTIVE', created_at, @office, FALSE,
       JSON_OBJECT('lockCause', NULL, 'auditLogId', NULL),
       CONCAT('M15.ASH:', id, ':', DATE_FORMAT(created_at, '%Y-%m-%dT%H:%i:%s.%f'))
FROM account WHERE username LIKE 'demo.nguoithan.%';

INSERT INTO permission_assignment (kind, applies_to, account_id, role, effective_from, actor_id, reason)
SELECT 'ROLE_ASSIGNMENT', 'ACCOUNT', id, 'AC-10', created_at, @office, 'Dữ liệu mẫu demo người thân (góp ý demo BF-10)'
FROM account WHERE username LIKE 'demo.nguoithan.%';

DROP TEMPORARY TABLE demo_stay;
