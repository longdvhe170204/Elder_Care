-- Dữ liệu demo (chỉ profile demo; chủ dự án duyệt 2026-11-25: dữ liệu demo đầy đủ để chạy hết quy trình của viện):
-- danh mục vận hành chưa có ở V900_05.
--   1. Mốc sinh hoạt cố định của toàn viện (005 FR-056, Q-440; BF-02 bước 0): catalog_item M04.DAILY_ROUTINE, mỗi mốc một
--      phiên bản áp dụng từ hôm trước ngày nạp (DailyRoutineAttributes.versions).
--      Không có mốc bữa ăn hay uống thuốc: Lịch chăm sóc hằng ngày đã ghép giờ bữa của 011 và không hiện thuốc (FR-055).
--   2. Nhóm sở thích (014): catalog_item M04.INTEREST_GROUP.
--   3. Tài sản (019): loại tài sản ngoài hai loại hệ thống Giường, Xe; ghi tăng tài sản cho mọi giường như lệnh Tạo giường
--      (BedAssets.recordIncreaseForBed: nguồn BED, bộ phận Chăm sóc mã CARE, vị trí phòng, dòng lịch sử LOCATION); hai xe
--      (biển số, số chỗ, hạn đăng kiểm, bảo hiểm; màn Lịch xe cần xe); vài thiết bị có chu kỳ bảo trì. Nguồn tặng, chuyển
--      đến ghi mô tả nguồn (chk_asset_source_described); không ghi nguồn Mua mới vì cần yêu cầu mua sắm.
-- Mẫu kiểm tra chất lượng không có: danh sách kiểm tra lập theo từng ca (014 FR-024). Cú pháp dùng được ở MySQL 8.0 và 8.4.

SET @demo_at = UTC_TIMESTAMP(6);
SET @today = DATE(@demo_at + INTERVAL 7 HOUR);
SET @from = @today - INTERVAL 1 DAY;
SET @manager_staff = 9001;
-- asset.created_at theo giờ Việt Nam (AssetStore: LocalDateTime.ofInstant(at, BusinessTime.ZONE))
SET @asset_at = @demo_at + INTERVAL 7 HOUR;

-- 005: mốc sinh hoạt cố định
INSERT INTO catalog_item (catalog_type, module, name, sort_order, status, attributes)
SELECT 'M04.DAILY_ROUTINE', 'M04', r.name, r.sort_order, 'ACTIVE',
       JSON_OBJECT('versions', JSON_ARRAY(JSON_OBJECT('from', DATE_FORMAT(@from, '%Y-%m-%d'), 'name', r.name,
                                                      'time', r.at, 'active', TRUE)))
FROM (SELECT 'Thức dậy' AS name, '05:30' AS at, 1 AS sort_order
      UNION ALL SELECT 'Vệ sinh cá nhân', '06:00', 2
      UNION ALL SELECT 'Thể dục buổi sáng', '06:15', 3
      UNION ALL SELECT 'Hoạt động buổi sáng', '09:00', 4
      UNION ALL SELECT 'Ngủ trưa', '12:00', 5
      UNION ALL SELECT 'Hoạt động buổi chiều', '15:30', 6
      UNION ALL SELECT 'Sinh hoạt buổi tối', '19:00', 7
      UNION ALL SELECT 'Đi ngủ', '21:00', 8) r;

-- 014: nhóm sở thích
INSERT INTO catalog_item (catalog_type, module, name, sort_order, status, attributes) VALUES
    ('M04.INTEREST_GROUP', 'M04', 'Câu lạc bộ cờ tướng', 1, 'ACTIVE', NULL),
    ('M04.INTEREST_GROUP', 'M04', 'Hát karaoke, dân ca', 2, 'ACTIVE', NULL),
    ('M04.INTEREST_GROUP', 'M04', 'Trồng cây, làm vườn', 3, 'ACTIVE', NULL),
    ('M04.INTEREST_GROUP', 'M04', 'Đọc báo, nghe thời sự', 4, 'ACTIVE', NULL),
    ('M04.INTEREST_GROUP', 'M04', 'Dưỡng sinh, khí công', 5, 'ACTIVE', NULL);

-- 019: loại tài sản
INSERT INTO catalog_item (catalog_type, module, name, sort_order, status, attributes) VALUES
    ('M03.ASSET_TYPE', 'M03', 'Thiết bị y tế', 3, 'ACTIVE', JSON_OBJECT('system', FALSE)),
    ('M03.ASSET_TYPE', 'M03', 'Thiết bị hỗ trợ di chuyển', 4, 'ACTIVE', JSON_OBJECT('system', FALSE)),
    ('M03.ASSET_TYPE', 'M03', 'Thiết bị bếp', 5, 'ACTIVE', JSON_OBJECT('system', FALSE)),
    ('M03.ASSET_TYPE', 'M03', 'Thiết bị giặt là', 6, 'ACTIVE', JSON_OBJECT('system', FALSE));

-- Mã bộ phận logic (StaffingCatalogStore.departmentId: id cũ của bảng department nếu có)
CREATE TEMPORARY TABLE demo_department (name VARCHAR(60) PRIMARY KEY, logical_id BIGINT);
INSERT INTO demo_department (name, logical_id)
SELECT d.name, COALESCE(CAST(d.attributes ->> '$.legacy.id' AS UNSIGNED), d.id)
FROM catalog_item d
WHERE d.catalog_type = 'M09.DEPARTMENT' AND d.status = 'ACTIVE';

SET @care = (SELECT logical_id FROM demo_department WHERE name = 'Chăm sóc');
SET @bed_type = (SELECT id FROM catalog_item WHERE catalog_type = 'M03.ASSET_TYPE' AND code = 'BED');

-- Ghi tăng tài sản cho mọi giường chưa có tài sản
INSERT INTO asset (code, type_id, name, department_id, source, bed_id, status, location_type, location_id,
                   floor_area_id, created_by_staff_id, created_at)
SELECT CONCAT('BED-', b.id), @bed_type, CONCAT('Giường ', b.code, ', ', r.name), @care, 'BED', b.id, 'READY', 'ROOM',
       r.id, r.floor_id, @manager_staff, @asset_at
FROM bed b JOIN room r ON r.id = b.room_id
WHERE NOT EXISTS (SELECT 1 FROM asset a WHERE a.bed_id = b.id);

-- Xe và thiết bị
INSERT INTO asset (code, type_id, name, department_id, source, source_description, in_service_on,
                   maintenance_interval_days, maintenance_due_on, license_plate, seat_count, vehicle_inspection_due_on,
                   insurance_due_on, status, location_type, location_id, floor_area_id, created_by_staff_id,
                   created_at)
SELECT a.code, t.id, a.name, d.logical_id, a.source, a.source_description, '2025-03-01', a.interval_days,
       IF(a.interval_days IS NULL, NULL, @today + INTERVAL a.due_in DAY), a.plate, a.seats,
       IF(a.plate IS NULL, NULL, @today + INTERVAL 120 DAY), IF(a.plate IS NULL, NULL, @today + INTERVAL 45 DAY),
       'READY', a.location_type, a.location_id, a.floor_id, @manager_staff, @asset_at
FROM (SELECT 'XE-01' AS code, 'VEHICLE' AS type_code, NULL AS type_name, 'Xe cứu thương Ford Transit' AS name,
             'Hành chính – Kế toán' AS department, 'DONATED' AS source,
             'Sở Y tế tặng năm 2025 (dữ liệu demo)' AS source_description, 180 AS interval_days, 30 AS due_in,
             '29A-123.45' AS plate, 6 AS seats, 'PARKING_LOT' AS location_type, NULL AS location_id, NULL AS floor_id
      UNION ALL SELECT 'XE-02', 'VEHICLE', NULL, 'Xe 16 chỗ Hyundai Solati', 'Hành chính – Kế toán',
                       'TRANSFERRED_IN', 'Chuyển từ cơ sở cũ (dữ liệu demo)', 180, 60, '29B-678.90', 16,
                       'PARKING_LOT', NULL, NULL
      UNION ALL SELECT 'YT-01', NULL, 'Thiết bị y tế', 'Máy đo huyết áp điện tử tầng 1', 'Y tế', 'DONATED',
                       'Nhà tài trợ tặng (dữ liệu demo)', 90, 10, NULL, NULL, 'FLOOR', 9001, 9001
      UNION ALL SELECT 'YT-02', NULL, 'Thiết bị y tế', 'Máy đo SpO2 tầng 2', 'Y tế', 'DONATED',
                       'Nhà tài trợ tặng (dữ liệu demo)', 90, 40, NULL, NULL, 'FLOOR', 9002, 9002
      UNION ALL SELECT 'YT-03', NULL, 'Thiết bị y tế', 'Máy tạo oxy Nhà B', 'Y tế', 'TRANSFERRED_IN',
                       'Chuyển từ cơ sở cũ (dữ liệu demo)', 60, 5, NULL, NULL, 'FLOOR', 9004, 9004
      UNION ALL SELECT 'DC-01', NULL, 'Thiết bị hỗ trợ di chuyển', 'Xe lăn tầng 1', 'Chăm sóc', 'DONATED',
                       'Hội người cao tuổi tặng (dữ liệu demo)', 180, 90, NULL, NULL, 'FLOOR', 9001, 9001
      UNION ALL SELECT 'DC-02', NULL, 'Thiết bị hỗ trợ di chuyển', 'Xe lăn Nhà B', 'Chăm sóc', 'DONATED',
                       'Hội người cao tuổi tặng (dữ liệu demo)', 180, 120, NULL, NULL, 'FLOOR', 9004, 9004
      UNION ALL SELECT 'BEP-01', NULL, 'Thiết bị bếp', 'Tủ lạnh bếp', 'Bếp', 'TRANSFERRED_IN',
                       'Chuyển từ cơ sở cũ (dữ liệu demo)', 365, 200, NULL, NULL, 'AREA', 9001, NULL
      UNION ALL SELECT 'GL-01', NULL, 'Thiết bị giặt là', 'Máy giặt công nghiệp', 'Chăm sóc', 'DONATED',
                       'Nhà tài trợ tặng (dữ liệu demo)', 180, 15, NULL, NULL, 'AREA', 9001, NULL) a
         JOIN catalog_item t ON t.catalog_type = 'M03.ASSET_TYPE'
    AND ((a.type_code IS NOT NULL AND t.code = a.type_code) OR (a.type_code IS NULL AND t.name = a.type_name))
         JOIN demo_department d ON d.name = a.department;

-- Lịch sử vị trí (M03.ASSET, LOCATION) của mọi tài sản vừa ghi tăng
INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value, value_id, starts_at, command,
                            actor_id, by_system, basis_type, basis_id, details, floor_area_id)
SELECT 'M03.ASSET', a.id, 'LOCATION', 'M03', 'PERIOD', a.location_type, a.location_id, @demo_at, 'CREATE',
       @manager_staff, FALSE, IF(a.bed_id IS NULL, NULL, 'M03.BED'), a.bed_id,
       JSON_OBJECT('type', a.location_type, 'id', a.location_id, 'reason', NULL), a.floor_area_id
FROM asset a
WHERE a.created_at = @asset_at;

DROP TEMPORARY TABLE demo_department;
