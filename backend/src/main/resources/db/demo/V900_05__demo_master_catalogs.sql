-- Dữ liệu gốc demo (chủ dự án yêu cầu 2026-11-19): chỉ nạp ở profile demo. Các danh mục mà luồng nghiệp vụ trên web cần
-- có sẵn trước khi thao tác, làm thay Quản lý viện, Bác sĩ, Hành chính phần khai báo danh mục:
-- dị nguyên (001), thuốc (006), dịch vụ và đơn giá (004, 010), loại công việc và giá trị kết quả chăm sóc (005), loại hoạt
-- động (014), chế độ ăn và loại đồ ăn bị cấm (011), vị trí lưu giữ đồ dùng (013), số liên hệ của cơ sở (007), khu nghỉ
-- bán trú (003).
-- Không gồm: món ăn và thực đơn (món cần Bác sĩ xác nhận phiên bản, thực đơn có máy trạng thái: tạo bằng ứng dụng), mẫu
-- kiểm tra chất lượng, nhóm sở thích. Không có loại công việc đo chỉ số (kết quả MEASURED_VALUE): chưa có nguồn ghi chỉ
-- số cho công việc chăm sóc.
-- Id đặt từ 9101 để không trùng dữ liệu đã có và để các danh mục tham chiếu nhau (thuốc → dị nguyên, dịch vụ → thuốc,
-- loại công việc → dịch vụ). Thuộc tính JSON theo lớp *Attributes của từng module. Cú pháp dùng được ở MySQL 8.0 và 8.4.

-- 001: dị nguyên (nhóm hoạt chất thuốc, thực phẩm, khác)
INSERT INTO catalog_item (id, catalog_type, module, name, sort_order, status, attributes) VALUES
    (9101, 'M01.ALLERGEN', 'M01', 'Paracetamol', 0, 'ACTIVE', JSON_OBJECT('allergenGroup', 'ACTIVE_INGREDIENT')),
    (9102, 'M01.ALLERGEN', 'M01', 'Amlodipin', 0, 'ACTIVE', JSON_OBJECT('allergenGroup', 'ACTIVE_INGREDIENT')),
    (9103, 'M01.ALLERGEN', 'M01', 'Metformin', 0, 'ACTIVE', JSON_OBJECT('allergenGroup', 'ACTIVE_INGREDIENT')),
    (9104, 'M01.ALLERGEN', 'M01', 'Amoxicillin', 0, 'ACTIVE', JSON_OBJECT('allergenGroup', 'ACTIVE_INGREDIENT')),
    (9105, 'M01.ALLERGEN', 'M01', 'Omeprazol', 0, 'ACTIVE', JSON_OBJECT('allergenGroup', 'ACTIVE_INGREDIENT')),
    (9111, 'M01.ALLERGEN', 'M01', 'Tôm', 0, 'ACTIVE', JSON_OBJECT('allergenGroup', 'FOOD')),
    (9112, 'M01.ALLERGEN', 'M01', 'Cua', 0, 'ACTIVE', JSON_OBJECT('allergenGroup', 'FOOD')),
    (9113, 'M01.ALLERGEN', 'M01', 'Đậu phộng', 0, 'ACTIVE', JSON_OBJECT('allergenGroup', 'FOOD')),
    (9114, 'M01.ALLERGEN', 'M01', 'Sữa bò', 0, 'ACTIVE', JSON_OBJECT('allergenGroup', 'FOOD')),
    (9115, 'M01.ALLERGEN', 'M01', 'Trứng', 0, 'ACTIVE', JSON_OBJECT('allergenGroup', 'FOOD')),
    (9121, 'M01.ALLERGEN', 'M01', 'Phấn hoa', 0, 'ACTIVE', JSON_OBJECT('allergenGroup', 'OTHER'));

-- 006: danh mục thuốc; tên dòng và mã như MedicationRepository ghi (mã là SHA-256 của tên|hàm lượng|dạng bào chế)
INSERT INTO catalog_item (id, catalog_type, module, code, name, sort_order, status, attributes)
SELECT m.id, 'M07.MEDICATION', 'M07',
       CONCAT('M07-', LEFT(SHA2(LOWER(CONCAT(m.name, '|', m.concentration, '|', m.form)), 256), 35)),
       CONCAT(m.name, ' | ', m.concentration, ' | ', m.form), 0, 'ACTIVE',
       JSON_OBJECT('name', m.name, 'concentration', m.concentration, 'dosageForm', m.form, 'defaultRoute', 'Uống',
                   'usageUnit', m.unit, 'beforeWindowMinutes', NULL, 'afterWindowMinutes', NULL,
                   'specialControl', CAST('false' AS JSON), 'activeIngredientIds', JSON_ARRAY(m.ingredient),
                   'legacy', NULL)
FROM (SELECT 9201 AS id, 'Paracetamol' AS name, '500 mg' AS concentration, 'Viên nén' AS form, 'viên' AS unit,
             9101 AS ingredient
      UNION ALL SELECT 9202, 'Amlodipin', '5 mg', 'Viên nén', 'viên', 9102
      UNION ALL SELECT 9203, 'Metformin', '500 mg', 'Viên nén', 'viên', 9103
      UNION ALL SELECT 9204, 'Amoxicillin', '500 mg', 'Viên nang', 'viên', 9104
      UNION ALL SELECT 9205, 'Omeprazol', '20 mg', 'Viên nang', 'viên', 9105) m;

-- 004, 010: dịch vụ và đơn giá mở từ 2025-01-01. Mỗi mức chăm sóc một dịch vụ CARE; phí lưu trú theo hai loại phòng
-- của V900_01; thuốc nguồn viện gắn Paracetamol.
INSERT INTO catalog_item (id, catalog_type, module, name, sort_order, status, attributes)
SELECT s.id, 'M02.SERVICE', 'M02', s.name, 0, 'ACTIVE',
       JSON_OBJECT('type', s.type, 'unit', s.unit, 'billingPeriod', s.period, 'careLevelCode', s.care_level,
                   'roomTypeId', s.room_type_id, 'medicationId', s.medication_id)
FROM (SELECT 9301 AS id, 'Chăm sóc cơ bản' AS name, 'CARE' AS type, 'tháng' AS unit, 'MONTH' AS period,
             'BASIC' AS care_level, NULL AS room_type_id, NULL AS medication_id
      UNION ALL SELECT 9302, 'Chăm sóc thường xuyên', 'CARE', 'tháng', 'MONTH', 'FREQUENT', NULL, NULL
      UNION ALL SELECT 9303, 'Chăm sóc đặc biệt', 'CARE', 'tháng', 'MONTH', 'SPECIAL', NULL, NULL
      UNION ALL SELECT 9311, 'Phí lưu trú phòng đôi', 'RESIDENCY_FEE', 'tháng', 'MONTH', NULL, 9001, NULL
      UNION ALL SELECT 9312, 'Phí lưu trú phòng bốn giường', 'RESIDENCY_FEE', 'tháng', 'MONTH', NULL, 9002, NULL
      UNION ALL SELECT 9321, 'Bán trú theo buổi', 'DAY_CARE_SESSION', 'buổi', 'SESSION', NULL, NULL, NULL
      UNION ALL SELECT 9322, 'Suất ăn', 'MEAL', 'ngày', 'DAY', NULL, NULL, NULL
      UNION ALL SELECT 9323, 'Phục hồi chức năng', 'REHABILITATION', 'buổi', 'SESSION', NULL, NULL, NULL
      UNION ALL SELECT 9324, 'Hoạt động ngoài trời', 'ACTIVITY', 'lần', 'OCCURRENCE', NULL, NULL, NULL
      UNION ALL SELECT 9325, 'Đưa đi khám bệnh', 'MEDICAL_ESCORT', 'lần', 'OCCURRENCE', NULL, NULL, NULL
      UNION ALL SELECT 9326, 'Tiêm theo y lệnh', 'INJECTION', 'lần', 'OCCURRENCE', NULL, NULL, NULL
      UNION ALL SELECT 9327, 'Người thân ở lại qua đêm', 'FAMILY_STAY', 'đêm', 'DAY', NULL, NULL, NULL
      UNION ALL SELECT 9328, 'Paracetamol 500 mg nguồn viện', 'FACILITY_SUPPLIED_MEDICATION', 'viên', 'UNIT', NULL,
                       NULL, 9201
      UNION ALL SELECT 9329, 'Tã người lớn', 'CONSUMABLE', 'miếng', 'UNIT', NULL, NULL, NULL
      UNION ALL SELECT 9330, 'Găng tay y tế', 'MEDICAL_SUPPLY', 'đôi', 'UNIT', NULL, NULL, NULL) s;

INSERT INTO price_version (service_id, unit_price, effective_from, effective_to) VALUES
    (9301, 3000000, '2025-01-01', NULL), (9302, 5000000, '2025-01-01', NULL), (9303, 8000000, '2025-01-01', NULL),
    (9311, 4000000, '2025-01-01', NULL), (9312, 2500000, '2025-01-01', NULL),
    (9321, 250000, '2025-01-01', NULL), (9322, 60000, '2025-01-01', NULL), (9323, 200000, '2025-01-01', NULL),
    (9324, 100000, '2025-01-01', NULL), (9325, 300000, '2025-01-01', NULL), (9326, 50000, '2025-01-01', NULL),
    (9327, 150000, '2025-01-01', NULL), (9328, 2000, '2025-01-01', NULL), (9329, 15000, '2025-01-01', NULL),
    (9330, 5000, '2025-01-01', NULL);

-- 005: loại công việc chăm sóc; khung giờ null là đọc CFG khi lập mục
INSERT INTO catalog_item (id, catalog_type, module, name, sort_order, status, attributes)
SELECT t.id, 'M04.TASK_TYPE', 'M04', t.name, t.id - 9400, 'ACTIVE',
       JSON_OBJECT('group', t.task_group, 'professional', CAST(IF(t.professional, 'true', 'false') AS JSON),
                   'resultType', t.result_type, 'defaultRole', t.role, 'defaultImportance', t.importance,
                   'windowBeforeMinutes', NULL, 'windowAfterMinutes', NULL,
                   'billable', CAST(IF(t.service_id IS NULL, 'false', 'true') AS JSON), 'serviceId', t.service_id,
                   'upperLimit', t.upper_limit, 'vitalSignType', NULL, 'certificates', JSON_ARRAY(), 'legacy', NULL)
FROM (SELECT 9401 AS id, 'Vệ sinh cá nhân' AS name, 'PERSONAL_HYGIENE' AS task_group, FALSE AS professional,
             'PERFORMED' AS result_type, 'CAREGIVER' AS role, 'NORMAL' AS importance, NULL AS service_id,
             NULL AS upper_limit
      UNION ALL SELECT 9402, 'Tắm', 'BATHING', FALSE, 'PERFORMED', 'CAREGIVER', 'IMPORTANT', NULL, NULL
      UNION ALL SELECT 9403, 'Thay quần áo', 'DRESSING', FALSE, 'PERFORMED', 'CAREGIVER', 'NORMAL', NULL, NULL
      UNION ALL SELECT 9404, 'Hỗ trợ ăn', 'EATING_ASSISTANCE', FALSE, 'EATING', 'CAREGIVER', 'IMPORTANT', NULL, NULL
      UNION ALL SELECT 9405, 'Hỗ trợ uống nước', 'DRINKING_ASSISTANCE', FALSE, 'ML', 'CAREGIVER', 'NORMAL', NULL,
                       NULL
      UNION ALL SELECT 9406, 'Hỗ trợ đi vệ sinh', 'TOILETING_ASSISTANCE', FALSE, 'PERFORMED', 'CAREGIVER', 'NORMAL',
                       NULL, NULL
      UNION ALL SELECT 9407, 'Hỗ trợ vận động', 'MOVEMENT', FALSE, 'MOVEMENT', 'CAREGIVER', 'NORMAL', NULL, NULL
      UNION ALL SELECT 9408, 'Tập phục hồi chức năng', 'REHABILITATION', TRUE, 'PERFORMED', 'NURSING_STAFF',
                       'IMPORTANT', 9323, NULL
      UNION ALL SELECT 9409, 'Theo dõi giấc ngủ', 'SLEEP', FALSE, 'SLEEP', 'CAREGIVER', 'NORMAL', NULL, NULL
      UNION ALL SELECT 9410, 'Theo dõi tâm trạng', 'MOOD', FALSE, 'MOOD', 'CAREGIVER', 'NORMAL', NULL, NULL
      UNION ALL SELECT 9411, 'Theo dõi hành vi', 'BEHAVIOR', FALSE, 'BEHAVIOR', 'CAREGIVER', 'NORMAL', NULL, NULL
      UNION ALL SELECT 9412, 'Thay tã', 'DIAPER_ITEM_CHANGE', FALSE, 'ITEM_QUANTITY', 'CAREGIVER', 'NORMAL', 9329, 10
      UNION ALL SELECT 9413, 'Kiểm tra đầu ca', 'SHIFT_START_CHECK', FALSE, 'PERFORMED', 'CAREGIVER', 'NORMAL', NULL,
                       NULL) t;

-- 005: giá trị kết quả giấc ngủ, tâm trạng, hành vi; nhóm quy tắc INSOMNIA, NEGATIVE dùng cho cảnh báo theo dõi
INSERT INTO catalog_item (id, catalog_type, module, name, sort_order, status, attributes)
SELECT v.id, 'M04.RESULT_VALUE', 'M04', v.name, v.id - 9450, 'ACTIVE',
       JSON_OBJECT('type', v.value_type, 'code', v.code, 'ruleGroup', v.rule_group, 'legacy', NULL)
FROM (SELECT 9451 AS id, 'Ngủ ngon' AS name, 'SLEEP_QUALITY' AS value_type, 'GOOD' AS code, NULL AS rule_group
      UNION ALL SELECT 9452, 'Ngủ chập chờn', 'SLEEP_QUALITY', 'INTERRUPTED', NULL
      UNION ALL SELECT 9453, 'Mất ngủ', 'SLEEP_QUALITY', 'INSOMNIA', 'INSOMNIA'
      UNION ALL SELECT 9461, 'Vui vẻ', 'MOOD', 'CHEERFUL', NULL
      UNION ALL SELECT 9462, 'Tâm trạng bình thường', 'MOOD', 'NORMAL', NULL
      UNION ALL SELECT 9463, 'Buồn bã', 'MOOD', 'SAD', 'NEGATIVE'
      UNION ALL SELECT 9464, 'Lo âu', 'MOOD', 'ANXIOUS', 'NEGATIVE'
      UNION ALL SELECT 9471, 'Hành vi bình thường', 'BEHAVIOR', 'NORMAL', NULL
      UNION ALL SELECT 9472, 'Kích động', 'BEHAVIOR', 'AGITATED', 'NEGATIVE'
      UNION ALL SELECT 9473, 'Đi lang thang', 'BEHAVIOR', 'WANDERING', 'NEGATIVE') v;

-- 014: loại hoạt động
INSERT INTO catalog_item (id, catalog_type, module, name, sort_order, status, attributes) VALUES
    (9501, 'M04.ACTIVITY_TYPE', 'M04', 'Thể dục buổi sáng', 0, 'ACTIVE',
     JSON_OBJECT('groupActivity', CAST('true' AS JSON), 'offSite', CAST('false' AS JSON),
                 'interestGroupIds', JSON_ARRAY())),
    (9502, 'M04.ACTIVITY_TYPE', 'M04', 'Sinh hoạt văn nghệ', 0, 'ACTIVE',
     JSON_OBJECT('groupActivity', CAST('true' AS JSON), 'offSite', CAST('false' AS JSON),
                 'interestGroupIds', JSON_ARRAY())),
    (9503, 'M04.ACTIVITY_TYPE', 'M04', 'Dã ngoại công viên', 0, 'ACTIVE',
     JSON_OBJECT('groupActivity', CAST('true' AS JSON), 'offSite', CAST('true' AS JSON),
                 'interestGroupIds', JSON_ARRAY())),
    (9504, 'M04.ACTIVITY_TYPE', 'M04', 'Đọc sách cá nhân', 0, 'ACTIVE',
     JSON_OBJECT('groupActivity', CAST('false' AS JSON), 'offSite', CAST('false' AS JSON),
                 'interestGroupIds', JSON_ARRAY()));

-- 011: chế độ ăn (đúng một mặc định, mặc định không liên quan điều trị); chưa có món nên chưa có món an toàn
INSERT INTO catalog_item (id, catalog_type, module, name, sort_order, status, attributes) VALUES
    (9601, 'M08.DIET', 'M08', 'Ăn thường', 0, 'ACTIVE',
     JSON_OBJECT('description', 'Chế độ ăn mặc định', 'treatmentRelated', CAST('false' AS JSON),
                 'isDefault', CAST('true' AS JSON), 'restrictions', JSON_ARRAY(), 'safeDishId', NULL)),
    (9602, 'M08.DIET', 'M08', 'Ăn mềm', 0, 'ACTIVE',
     JSON_OBJECT('description', 'Cháo, súp, món nấu nhừ', 'treatmentRelated', CAST('false' AS JSON),
                 'isDefault', CAST('false' AS JSON), 'restrictions', JSON_ARRAY(), 'safeDishId', NULL)),
    (9603, 'M08.DIET', 'M08', 'Ăn cho người tiểu đường', 0, 'ACTIVE',
     JSON_OBJECT('description', 'Hạn chế đường, tinh bột nhanh', 'treatmentRelated', CAST('true' AS JSON),
                 'isDefault', CAST('false' AS JSON), 'restrictions', JSON_ARRAY(), 'safeDishId', NULL)),
    (9604, 'M08.DIET', 'M08', 'Không hải sản', 0, 'ACTIVE',
     JSON_OBJECT('description', 'Không tôm, cua', 'treatmentRelated', CAST('false' AS JSON),
                 'isDefault', CAST('false' AS JSON), 'restrictions', JSON_ARRAY(9111, 9112), 'safeDishId', NULL));

INSERT INTO catalog_item (id, catalog_type, module, name, sort_order, status, attributes) VALUES
    (9611, 'M08.FORBIDDEN_FOOD_TYPE', 'M08', 'Đồ uống có cồn', 0, 'ACTIVE',
     JSON_OBJECT('description', 'Rượu, bia và đồ uống có cồn')),
    (9612, 'M08.FORBIDDEN_FOOD_TYPE', 'M08', 'Thực phẩm tươi sống', 0, 'ACTIVE',
     JSON_OBJECT('description', 'Thịt, cá, hải sản chưa nấu chín')),
    (9613, 'M08.FORBIDDEN_FOOD_TYPE', 'M08', 'Đồ ăn không rõ nguồn gốc', 0, 'ACTIVE',
     JSON_OBJECT('description', 'Không có nhãn, không rõ nơi sản xuất'));

-- 013: vị trí lưu giữ đồ dùng cá nhân
INSERT INTO catalog_item (id, catalog_type, module, name, sort_order, status, attributes) VALUES
    (9701, 'M12.STORAGE_LOCATION', 'M12', 'Tủ lưu giữ tầng 1', 0, 'ACTIVE',
     JSON_OBJECT('description', 'Phòng trực tầng 1', 'floorAreaId', 9001)),
    (9702, 'M12.STORAGE_LOCATION', 'M12', 'Tủ lưu giữ tầng 2', 0, 'ACTIVE',
     JSON_OBJECT('description', 'Phòng trực tầng 2', 'floorAreaId', 9002)),
    (9703, 'M12.STORAGE_LOCATION', 'M12', 'Két văn phòng hành chính', 0, 'ACTIVE',
     JSON_OBJECT('description', 'Tiền mặt, giấy tờ, đồ giá trị', 'floorAreaId', NULL));

-- 007: số liên hệ của cơ sở (mã CURRENT, FacilitySources đọc)
INSERT INTO catalog_item (id, catalog_type, module, code, name, sort_order, status, attributes) VALUES
    (9801, 'M06.FACILITY_CONTACT', 'M06', 'CURRENT', 'Số liên hệ của cơ sở', 0, 'ACTIVE',
     JSON_OBJECT('phoneNumber', '02400000000', 'note', 'Số trực 24/7 của viện (demo)'));

-- 003: khu nghỉ bán trú ở tầng 1 (đúng một trong tầng, khu vực)
INSERT INTO catalog_item (id, catalog_type, module, name, sort_order, status, attributes) VALUES
    (9802, 'M03.DAY_CARE_REST_AREA', 'M03', 'Khu nghỉ bán trú tầng 1', 0, 'ACTIVE',
     JSON_OBJECT('code', 'BT-T1', 'floorId', 9001, 'areaId', NULL));
