-- Dữ liệu demo phần lưu trú và ca (chủ dự án chọn 2026-11-17; quickstart 002 mục 1): chỉ nạp ở profile demo.
-- Gồm: giường của bốn phòng ở V900_01; người cao tuổi A (9001) ở P101 tầng 1, B (9002) ở P201 tầng 2 chuyển sang
-- Đang lưu trú, mức Chăm sóc cơ bản, hợp đồng nội trú dài hạn Hiệu lực từ 30 ngày trước, phân bổ giường Đang hiệu lực;
-- ca ngày 07:00–18:00 tầng 1 đã công bố cho 7 ngày kể từ ngày nạp, Người phụ trách ca demo.dieuduong (9005), thành viên
-- demo.dieuduong, demo.chamsoc (9006); phân công A cho demo.chamsoc và demo.dieuduong (Điều dưỡng phụ trách) trong
-- từng ca; Trưởng tầng demo.truongtang (9003) giao tầng 1.
-- Ghi như các lệnh của ứng dụng ghi (dòng lịch sử trạng thái, lịch sử giường), theo mẫu fixture test chạy với bean thật
-- (RealCareFixture). Không gồm: ghi tăng tài sản của giường (019), tệp scan hợp đồng thật (scan_file_id trỏ id 9001
-- chưa có tệp), bản đồng ý, đánh giá của bác sĩ: lệnh tài sản trên các giường này và xem tệp scan chưa dùng được.
-- Ca chỉ có cho 7 ngày kể từ ngày nạp; sau đó lập ca bằng ứng dụng.

SET @demo_at = UTC_TIMESTAMP(6);
SET @since = @demo_at - INTERVAL 30 DAY;
SET @start_date = DATE(@since + INTERVAL 7 HOUR);
SET @today = DATE(@demo_at + INTERVAL 7 HOUR);
SET @office = (SELECT id FROM account WHERE username = 'demo.hanhchinh');
SET @manager = (SELECT id FROM account WHERE username = 'demo.quanly');

-- 003: giường; G1 của P101 (A) và P201 (B) Đang sử dụng, còn lại Trống
INSERT INTO bed (id, room_id, code, state) VALUES
    (9001, 9001, 'G1', 'OCCUPIED'), (9002, 9001, 'G2', 'EMPTY'),
    (9003, 9002, 'G1', 'EMPTY'), (9004, 9002, 'G2', 'EMPTY'), (9005, 9002, 'G3', 'EMPTY'), (9006, 9002, 'G4', 'EMPTY'),
    (9007, 9003, 'G1', 'OCCUPIED'), (9008, 9003, 'G2', 'EMPTY'),
    (9009, 9004, 'G1', 'EMPTY'), (9010, 9004, 'G2', 'EMPTY'), (9011, 9004, 'G3', 'EMPTY'), (9012, 9004, 'G4', 'EMPTY');

INSERT INTO bed_state_history (bed_id, from_state, to_state, event, occurred_at, actor_id, basis_type, basis_id,
                               floor_area_id)
SELECT b.id, NULL, 'EMPTY', 'CREATE_BED', @since - INTERVAL 1 DAY, @manager, 'BED', b.id, r.floor_id
FROM bed b JOIN room r ON r.id = b.room_id
WHERE b.id BETWEEN 9001 AND 9012;

-- 001: A, B Đang lưu trú, mức Chăm sóc cơ bản (dòng lịch sử như lệnh Hoàn tất tiếp nhận, ghi mức chăm sóc)
UPDATE resident SET status = 'IN_RESIDENCE', status_since = @since, current_care_level = 'BASIC'
WHERE id IN (9001, 9002);

INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value_before, value, starts_at, command,
                            actor_id, by_system)
SELECT 'M01.RESIDENT', r.id, 'STATUS', 'M01', 'POINT', 'ADMISSION_IN_PROGRESS', 'IN_RESIDENCE', @since,
       'COMPLETE_ADMISSION', @office, FALSE
FROM resident r WHERE r.id IN (9001, 9002);

INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value, starts_at, actor_id, by_system)
SELECT 'M01.RESIDENT', r.id, 'CARE_LEVEL', 'M01', 'POINT', 'BASIC', @since, @manager, FALSE
FROM resident r WHERE r.id IN (9001, 9002);

-- 004: hợp đồng nội trú dài hạn Hiệu lực, người ký là người đại diện R (9001) cho A; B chưa có người thân trong demo
INSERT INTO contract (id, contract_number, resident_id, residence_type, start_date, end_date, care_level,
                      residency_fee_billing_period, status, signed_date, signing_representative_id, scan_file_id,
                      created_by, created_at) VALUES
    (9001, CONCAT('HD-', YEAR(@start_date), '-009001'), 9001, 'LONG_TERM_RESIDENTIAL', @start_date, NULL, 'BASIC',
     'MONTH', 'ACTIVE', @start_date, 9001, 9001, @office, @since),
    (9002, CONCAT('HD-', YEAR(@start_date), '-009002'), 9002, 'LONG_TERM_RESIDENTIAL', @start_date, NULL, 'BASIC',
     'MONTH', 'ACTIVE', @start_date, 9001, 9001, @office, @since);

INSERT INTO contract_version (kind, contract_id, services, arrival_schedule, created_at)
SELECT 'DRAFT', c.id, '[]', '[]', @since FROM contract c WHERE c.id IN (9001, 9002);

INSERT INTO contract_version (kind, contract_id, sequence_no, effective_from, content, residence_type, care_level,
                              created_at, locked_at)
SELECT 'CONTENT', c.id, 1, @start_date,
       JSON_OBJECT('residenceType', 'LONG_TERM_RESIDENTIAL', 'careLevel', 'BASIC', 'startDate', @start_date,
                   'roomTypeId', 9001, 'services', JSON_ARRAY(), 'arrivalSchedule', JSON_ARRAY()),
       'LONG_TERM_RESIDENTIAL', 'BASIC', @since, @since
FROM contract c WHERE c.id IN (9001, 9002);

-- 003: phân bổ giường Đang hiệu lực từ ngày vào ở
INSERT INTO bed_allocation (id, resident_id, bed_id, status, start_at, checked_care_level, care_level_source) VALUES
    (9001, 9001, 9001, 'ACTIVE', @since, 'BASIC', 'CURRENT'),
    (9002, 9002, 9007, 'ACTIVE', @since, 'BASIC', 'CURRENT');

INSERT INTO bed_state_history (bed_id, from_state, to_state, event, occurred_at, actor_id, basis_type, basis_id,
                               floor_area_id)
SELECT a.bed_id, 'EMPTY', 'OCCUPIED', 'ALLOCATION_STARTED', @since, @office, 'BED_ALLOCATION', a.id, r.floor_id
FROM bed_allocation a JOIN bed b ON b.id = a.bed_id JOIN room r ON r.id = b.room_id
WHERE a.id IN (9001, 9002);

-- 008: Trưởng tầng tầng 1
INSERT INTO staff_assignment (kind, staff_id, floor_area_id, starts_at, assigned_by, status)
VALUES ('FLOOR_MANAGER', 9003, 9001, @since, @manager, 'ACTIVE');

-- 008: mẫu ca ngày, lịch ca tháng đã công bố, ca 07:00–18:00 giờ Việt Nam (00:00–11:00 UTC) trong 7 ngày
INSERT INTO catalog_item (id, catalog_type, module, name, status, attributes)
VALUES (9001, 'M09.SHIFT_TEMPLATE', 'M09', 'Ca ngày', 'ACTIVE',
        JSON_OBJECT('createdAt', @demo_at, 'startTime', '07:00:00', 'endTime', '18:00:00', 'scopeType', 'FLOOR',
                    'requiresHandover', CAST('false' AS JSON), 'originalName', 'Ca ngày'));

CREATE TEMPORARY TABLE demo_day (k INT PRIMARY KEY);
INSERT INTO demo_day (k) VALUES (0), (1), (2), (3), (4), (5), (6);

INSERT IGNORE INTO shift_schedule (month, scope_type, scope_id, status)
SELECT DISTINCT DATE_FORMAT(@today + INTERVAL k DAY, '%Y-%m'), 'FLOOR', 9001, 'PUBLISHED' FROM demo_day;

INSERT INTO work_shift (schedule_id, template_id, scope_type, scope_id, starts_at, ends_at, requires_handover, status)
SELECT s.id, 9001, 'FLOOR', 9001, TIMESTAMP(@today + INTERVAL d.k DAY), TIMESTAMP(@today + INTERVAL d.k DAY) + INTERVAL 11 HOUR,
       FALSE, 'PUBLISHED'
FROM demo_day d
         JOIN shift_schedule s ON s.month = DATE_FORMAT(@today + INTERVAL d.k DAY, '%Y-%m') AND s.scope_type = 'FLOOR'
    AND s.scope_id = 9001
ORDER BY d.k;

CREATE TEMPORARY TABLE demo_shift AS
SELECT id, starts_at, ends_at FROM work_shift WHERE template_id = 9001 AND scope_type = 'FLOOR' AND scope_id = 9001;

INSERT INTO shift_staff (shift_id, staff_id)
SELECT s.id, m.staff_id FROM demo_shift s JOIN (SELECT 9005 AS staff_id UNION ALL SELECT 9006) m;

INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value_id, starts_at, command, by_system)
SELECT 'M09.SHIFT', s.id, 'LEAD_STAFF', 'M09', 'PERIOD', 9005, s.starts_at, 'SCHEDULED', FALSE FROM demo_shift s;

-- 008: phân công A trong từng ca; ca đang diễn ra thì Đang hiệu lực, ca sau thì Đã lên lịch (như lệnh phân công)
INSERT INTO staff_assignment (shift_id, staff_id, target_type, target_id, role, assignment_kind, assigned_nurse,
                              starts_at, ends_at, status, created_by)
SELECT s.id, p.staff_id, 'RESIDENT', 9001, p.role, 'PRIMARY', p.nurse, s.starts_at, s.ends_at,
       IF(s.starts_at <= @demo_at, 'ACTIVE', 'SCHEDULED'), @manager
FROM demo_shift s
         JOIN (SELECT 9006 AS staff_id, 'CAREGIVER' AS role, FALSE AS nurse
               UNION ALL SELECT 9005, 'NURSING_STAFF', TRUE) p;

DROP TEMPORARY TABLE demo_shift;
DROP TEMPORARY TABLE demo_day;
