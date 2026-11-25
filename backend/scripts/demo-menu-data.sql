-- Dữ liệu thử màn lập và công bố thực đơn tuần (BF-03, Q-445) cho bản demo. KHÔNG phải migration: chạy tay vào schema
-- của bản demo (elder_care, hoặc MySQL trong docker compose) sau khi backend profile demo đã chạy ít nhất một lần.
-- Chạy lại nhiều lần không sinh trùng: dòng nào đã có (theo id, mã hay tên đang hiệu lực) thì bỏ qua.
--
-- Gồm:
--   1. Bốn bữa trong danh mục (M08.MEAL): Sáng, Trưa, Phụ chiều, Tối, kèm giờ bữa dự kiến ở CFG-M08-04.
--      Chưa có giờ bữa thì bữa không hiện ở thông tin trước khi gửi duyệt và không chốt suất được.
--   2. 18 món Đang dùng (có mô tả, chưa có ảnh: tải ảnh trên web để thử) và 1 món Nháp để thử "Đưa vào dùng".
--   3. Dị ứng trong hồ sơ sức khỏe do Bác sĩ demo ghi: A (9001) dị ứng Tôm, B (9002) dị ứng Sữa bò, để thấy số người
--      theo loại dị ứng (không tên) ở bảng thông tin trước khi gửi duyệt.
--
-- Sau khi chạy: KHỞI ĐỘNG LẠI backend (tham số được giữ trong bộ nhớ, không khởi động lại thì chưa thấy giờ bữa).
-- Chạy:  mysql -u <tài khoản> -p elder_care < backend/scripts/demo-menu-data.sql
-- Cú pháp dùng được ở MySQL 8.0 và 8.4.

SET NAMES utf8mb4;
SET @now = UTC_TIMESTAMP(6);
SET @manager = (SELECT id FROM account WHERE username = 'demo.quanly');
SET @doctor = (SELECT id FROM account WHERE username = 'demo.bacsi');

-- 1. Bữa ăn ------------------------------------------------------------------------------------------------------------
INSERT INTO catalog_item (id, catalog_type, module, code, name, sort_order, status, attributes, version)
SELECT m.id, 'M08.MEAL', 'M08', m.code, m.name, m.sort_order, 'ACTIVE',
       JSON_OBJECT('servesResidential', CAST('true' AS JSON), 'servesDayCare', CAST(m.day_care AS JSON)), 0
FROM (SELECT 9621 AS id, 'SANG' AS code, 'Bữa sáng' AS name, 1 AS sort_order, 'false' AS day_care
      UNION ALL SELECT 9622, 'TRUA', 'Bữa trưa', 2, 'true'
      UNION ALL SELECT 9623, 'PHU_CHIEU', 'Bữa phụ chiều', 3, 'true'
      UNION ALL SELECT 9624, 'TOI', 'Bữa tối', 4, 'false') m
WHERE NOT EXISTS (SELECT 1 FROM catalog_item c
                  WHERE c.id = m.id
                     OR (c.catalog_type = 'M08.MEAL' AND (c.code = m.code OR (c.status = 'ACTIVE' AND c.name = m.name))));

-- Giờ bữa dự kiến: khóa là mã của bữa đang hiệu lực có tên ở danh sách dưới (bữa viện đã tạo trước cũng dùng được);
-- bữa đã có giờ thì giữ nguyên giờ đó. Biến SQL không giữ kiểu JSON nên dùng CAST khi đọc lại.
SET @default_times = (
    SELECT JSON_OBJECTAGG(c.code, t.meal_time)
    FROM (SELECT 'Bữa sáng' AS name, '06:30' AS meal_time
          UNION ALL SELECT 'Bữa trưa', '11:00'
          UNION ALL SELECT 'Bữa phụ chiều', '14:30'
          UNION ALL SELECT 'Bữa tối', '17:00') t
             JOIN catalog_item c ON c.catalog_type = 'M08.MEAL' AND c.status = 'ACTIVE' AND c.name = t.name
    WHERE c.code IS NOT NULL);
SET @new_value = (
    SELECT JSON_SET(current_value, '$.mealTimes',
                    JSON_MERGE_PATCH(COALESCE(CAST(@default_times AS JSON), JSON_OBJECT()),
                                     IF(JSON_TYPE(JSON_EXTRACT(current_value, '$.mealTimes')) = 'OBJECT',
                                        JSON_EXTRACT(current_value, '$.mealTimes'), JSON_OBJECT())))
    FROM parameter WHERE code = 'CFG-M08-04');
SET @changed = (SELECT NOT (current_value <=> CAST(@new_value AS JSON)) FROM parameter WHERE code = 'CFG-M08-04');

-- Lịch sử tham số ở status_history loại M15.PARAMETER (như ParameterStore.addHistory): mốc sau dòng mới nhất
SET @effective_from = (SELECT GREATEST(@now, COALESCE(MAX(starts_at) + INTERVAL 1 MICROSECOND, @now))
                       FROM status_history
                       WHERE entity_type = 'M15.PARAMETER' AND entity_id = 0 AND attribute = 'CFG-M08-04');
INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, starts_at, command, actor_id, by_system,
                            reason, details, unique_key)
SELECT 'M15.PARAMETER', 0, 'CFG-M08-04', 'M15', 'POINT', @effective_from, 'CHANGE', @manager, FALSE,
       'Dữ liệu demo: giờ bữa dự kiến cho thực đơn', JSON_OBJECT('value', CAST(@new_value AS JSON)),
       CONCAT('M15.PARAMETER:CFG-M08-04:', DATE_FORMAT(@effective_from, '%Y-%m-%dT%H:%i:%s.%f'))
WHERE @changed;

UPDATE parameter
SET current_value = CAST(@new_value AS JSON), version = version + 1, changed_at = @now, changed_by = @manager
WHERE code = 'CFG-M08-04' AND @changed;

-- 2. Món ăn ------------------------------------------------------------------------------------------------------------
INSERT INTO catalog_item (id, catalog_type, module, code, name, sort_order, status, attributes, version)
SELECT d.id, 'M08.DISH', 'M08', NULL, d.name, 0, 'ACTIVE',
       JSON_OBJECT('status', d.status, 'description', d.description, 'imageFileId', NULL,
                   'everActive', CAST(IF(d.status = 'ACTIVE', 'true', 'false') AS JSON),
                   'deactivationReason', NULL, 'deactivatedBy', NULL, 'deactivatedAt', NULL), 0
FROM (SELECT 9631 AS id, 'Cháo thịt bằm' AS name, 'Cháo gạo tẻ nấu nhừ với thịt heo bằm, hành lá' AS description,
             'ACTIVE' AS status
      UNION ALL SELECT 9632, 'Phở bò', 'Bánh phở, thịt bò tái chín, nước dùng xương', 'ACTIVE'
      UNION ALL SELECT 9633, 'Bánh mì trứng ốp la', 'Bánh mì mềm, trứng gà ốp la, dưa leo', 'ACTIVE'
      UNION ALL SELECT 9634, 'Xôi đậu xanh', 'Xôi nếp đậu xanh, muối mè', 'ACTIVE'
      UNION ALL SELECT 9635, 'Bún riêu cua', 'Bún, riêu cua đồng, cà chua, đậu hũ', 'ACTIVE'
      UNION ALL SELECT 9636, 'Cơm trắng', 'Cơm gạo thơm nấu mềm', 'ACTIVE'
      UNION ALL SELECT 9637, 'Cá kho tộ', 'Cá basa kho tộ, ít mặn', 'ACTIVE'
      UNION ALL SELECT 9638, 'Thịt kho trứng', 'Thịt heo ba rọi kho nước dừa với trứng vịt', 'ACTIVE'
      UNION ALL SELECT 9639, 'Canh bí đỏ nấu tôm', 'Bí đỏ nấu với tôm bóc vỏ', 'ACTIVE'
      UNION ALL SELECT 9640, 'Rau muống luộc', 'Rau muống luộc chấm nước tương', 'ACTIVE'
      UNION ALL SELECT 9641, 'Gà luộc lá chanh', 'Thịt gà ta luộc, xé nhỏ, lá chanh', 'ACTIVE'
      UNION ALL SELECT 9642, 'Đậu hũ sốt cà chua', 'Đậu hũ non sốt cà chua, hành lá', 'ACTIVE'
      UNION ALL SELECT 9643, 'Canh chua cá lóc', 'Cá lóc, thơm, cà chua, giá, rau ngổ', 'ACTIVE'
      UNION ALL SELECT 9644, 'Tôm rim mặn ngọt', 'Tôm sú rim nước mắm, ít đường', 'ACTIVE'
      UNION ALL SELECT 9645, 'Canh rau ngót thịt bằm', 'Rau ngót nấu với thịt heo bằm', 'ACTIVE'
      UNION ALL SELECT 9646, 'Sữa chua', 'Sữa chua không đường', 'ACTIVE'
      UNION ALL SELECT 9647, 'Chè đậu xanh', 'Chè đậu xanh nước cốt dừa, ít ngọt', 'ACTIVE'
      UNION ALL SELECT 9648, 'Chuối tráng miệng', 'Chuối sứ chín', 'ACTIVE'
      UNION ALL SELECT 9649, 'Súp bí đỏ', 'Bí đỏ xay nhuyễn với sữa tươi (món Nháp, thử Đưa vào dùng)', 'DRAFT') d
WHERE NOT EXISTS (SELECT 1 FROM catalog_item c
                  WHERE c.id = d.id
                     OR (c.catalog_type = 'M08.DISH' AND c.status = 'ACTIVE' AND c.name = d.name));

-- 3. Dị ứng trong hồ sơ sức khỏe (Bác sĩ ghi, đã kiểm chứng) -------------------------------------------------------
INSERT INTO health_item (resident_id, type, allergen_id, severity_id, source_id, recorded_by, recorded_at,
                         pending_medication_check, pending_nutrition_check)
SELECT a.resident_id, 'ALLERGY', a.allergen_id,
       (SELECT id FROM catalog_item WHERE catalog_type = 'M01.ALLERGY_SEVERITY' AND name = 'Trung bình' LIMIT 1),
       (SELECT id FROM catalog_item WHERE catalog_type = 'M01.INFORMATION_SOURCE' AND name = 'Bác sĩ khám' LIMIT 1),
       @doctor, @now, FALSE, FALSE
FROM (SELECT 9001 AS resident_id, 9111 AS allergen_id
      UNION ALL SELECT 9002, 9114) a
WHERE EXISTS (SELECT 1 FROM resident r WHERE r.id = a.resident_id)
  AND EXISTS (SELECT 1 FROM catalog_item c WHERE c.id = a.allergen_id AND c.catalog_type = 'M01.ALLERGEN')
  AND NOT EXISTS (SELECT 1 FROM health_item h
                  WHERE h.resident_id = a.resident_id AND h.allergen_id = a.allergen_id AND h.status = 'ACTIVE');

-- Kiểm nhanh
SELECT catalog_type, COUNT(*) AS so_dong FROM catalog_item
WHERE catalog_type IN ('M08.MEAL', 'M08.DISH') AND status = 'ACTIVE' GROUP BY catalog_type;
SELECT JSON_EXTRACT(current_value, '$.mealTimes') AS gio_bua FROM parameter WHERE code = 'CFG-M08-04';
SELECT resident_id, allergen_id FROM health_item WHERE type = 'ALLERGY' AND resident_id IN (9001, 9002);
