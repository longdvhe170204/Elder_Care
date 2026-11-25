-- Dữ liệu demo (chỉ profile demo; chủ dự án duyệt 2026-11-25: dữ liệu demo đầy đủ để chạy hết quy trình của viện):
-- hạ tầng lớn hơn để thử phân bổ, chuyển giường, danh sách chờ, khoanh vùng theo tầng, khu. V900_01, V900_02 chỉ có Khu A
-- hai tầng, bốn phòng, 12 giường. Thêm:
--   1. Khu A Tầng 3 (9003); Khu B (9002, Nhà B) Tầng 1 (9004), Tầng 2 (9005);
--   2. loại phòng Phòng đơn (9003, mọi mức chăm sóc) và Phòng ba giường (9004, cơ bản, thường xuyên), kèm dịch vụ phí lưu
--      trú và đơn giá (004, 010) như V900_05;
--   3. 13 phòng (nam, nữ, không giới hạn), danh sách mức chăm sóc được phép lấy từ loại phòng (003 FR-004, như V900_07);
--   4. giường Trống theo số giường chuẩn của loại phòng, dòng lịch sử CREATE_BED như V900_02 (chưa ghi tăng tài sản 019);
--   5. tủ lưu giữ đồ gửi cho từng tầng mới (013, như V900_05).
-- Id phòng 9005 → 9017, giường từ 9101 để không trùng id đã cấp. Cú pháp dùng được ở MySQL 8.0 và 8.4.

SET @demo_at = UTC_TIMESTAMP(6);
SET @since = @demo_at - INTERVAL 30 DAY;
SET @manager = (SELECT id FROM account WHERE username = 'demo.quanly');

-- 003: khu, tầng
INSERT INTO area (id, code, name) VALUES (9002, 'KB', 'Khu B');

INSERT INTO floor (id, area_id, code, name, building_name) VALUES
    (9003, 9001, 'T3', 'Tầng 3', 'Nhà A'),
    (9004, 9002, 'T1', 'Tầng 1', 'Nhà B'),
    (9005, 9002, 'T2', 'Tầng 2', 'Nhà B');

-- 003: loại phòng mới
INSERT INTO room_type (id, name, standard_beds, serves_long_term, serves_short_term, allowed_care_levels) VALUES
    (9003, 'Phòng đơn', 1, TRUE, TRUE, JSON_ARRAY('BASIC', 'FREQUENT', 'SPECIAL')),
    (9004, 'Phòng ba giường', 3, TRUE, TRUE, JSON_ARRAY('BASIC', 'FREQUENT'));

-- 004, 010: phí lưu trú theo loại phòng mới
INSERT INTO catalog_item (catalog_type, module, name, sort_order, status, attributes)
VALUES ('M02.SERVICE', 'M02', 'Phí lưu trú phòng đơn', 0, 'ACTIVE',
        JSON_OBJECT('type', 'RESIDENCY_FEE', 'unit', 'tháng', 'billingPeriod', 'MONTH', 'careLevelCode', NULL,
                    'roomTypeId', 9003, 'medicationId', NULL));
INSERT INTO price_version (service_id, unit_price, effective_from, effective_to)
VALUES (LAST_INSERT_ID(), 6500000, '2025-01-01', NULL);

INSERT INTO catalog_item (catalog_type, module, name, sort_order, status, attributes)
VALUES ('M02.SERVICE', 'M02', 'Phí lưu trú phòng ba giường', 0, 'ACTIVE',
        JSON_OBJECT('type', 'RESIDENCY_FEE', 'unit', 'tháng', 'billingPeriod', 'MONTH', 'careLevelCode', NULL,
                    'roomTypeId', 9004, 'medicationId', NULL));
INSERT INTO price_version (service_id, unit_price, effective_from, effective_to)
VALUES (LAST_INSERT_ID(), 3200000, '2025-01-01', NULL);

-- 003: phòng
INSERT INTO room (id, floor_id, room_type_id, code, name, gender_policy) VALUES
    (9005, 9003, 9003, 'P301', 'Phòng 301', 'UNRESTRICTED'),
    (9006, 9003, 9001, 'P302', 'Phòng 302', 'FEMALE'),
    (9007, 9003, 9002, 'P303', 'Phòng 303', 'MALE'),
    (9008, 9003, 9004, 'P304', 'Phòng 304', 'UNRESTRICTED'),
    (9009, 9004, 9001, 'B101', 'Phòng B101', 'FEMALE'),
    (9010, 9004, 9001, 'B102', 'Phòng B102', 'MALE'),
    (9011, 9004, 9002, 'B103', 'Phòng B103', 'FEMALE'),
    (9012, 9004, 9003, 'B104', 'Phòng B104', 'UNRESTRICTED'),
    (9013, 9005, 9001, 'B201', 'Phòng B201', 'UNRESTRICTED'),
    (9014, 9005, 9004, 'B202', 'Phòng B202', 'MALE'),
    (9015, 9005, 9002, 'B203', 'Phòng B203', 'MALE'),
    (9016, 9005, 9003, 'B204', 'Phòng B204', 'UNRESTRICTED'),
    (9017, 9005, 9003, 'B205', 'Phòng B205', 'UNRESTRICTED');

UPDATE room r JOIN room_type t ON t.id = r.room_type_id
SET r.allowed_care_levels = t.allowed_care_levels
WHERE r.id BETWEEN 9005 AND 9017;

-- 003: giường Trống theo số giường chuẩn, mã G1 → Gn; id từ 9101
CREATE TEMPORARY TABLE demo_bed_no (n INT PRIMARY KEY);
INSERT INTO demo_bed_no (n) VALUES (1), (2), (3), (4);

INSERT INTO bed (id, room_id, code, state)
SELECT 9100 + ROW_NUMBER() OVER (ORDER BY r.id, b.n), r.id, CONCAT('G', b.n), 'EMPTY'
FROM room r JOIN room_type t ON t.id = r.room_type_id JOIN demo_bed_no b ON b.n <= t.standard_beds
WHERE r.id BETWEEN 9005 AND 9017;

INSERT INTO bed_state_history (bed_id, from_state, to_state, event, occurred_at, actor_id, basis_type, basis_id,
                               floor_area_id)
SELECT b.id, NULL, 'EMPTY', 'CREATE_BED', @since - INTERVAL 1 DAY, @manager, 'BED', b.id, r.floor_id
FROM bed b JOIN room r ON r.id = b.room_id
WHERE r.id BETWEEN 9005 AND 9017;

DROP TEMPORARY TABLE demo_bed_no;

-- 013: tủ lưu giữ đồ gửi của từng tầng mới
INSERT INTO catalog_item (catalog_type, module, name, sort_order, status, attributes) VALUES
    ('M12.STORAGE_LOCATION', 'M12', 'Tủ lưu giữ tầng 3', 0, 'ACTIVE',
     JSON_OBJECT('description', 'Phòng trực tầng 3', 'floorAreaId', 9003)),
    ('M12.STORAGE_LOCATION', 'M12', 'Tủ lưu giữ Nhà B tầng 1', 0, 'ACTIVE',
     JSON_OBJECT('description', 'Phòng trực Nhà B tầng 1', 'floorAreaId', 9004)),
    ('M12.STORAGE_LOCATION', 'M12', 'Tủ lưu giữ Nhà B tầng 2', 0, 'ACTIVE',
     JSON_OBJECT('description', 'Phòng trực Nhà B tầng 2', 'floorAreaId', 9005));
