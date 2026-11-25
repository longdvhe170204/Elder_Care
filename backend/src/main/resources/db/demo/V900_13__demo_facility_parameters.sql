-- Dữ liệu demo (chỉ profile demo; chủ dự án duyệt 2026-11-25: dữ liệu demo đầy đủ để chạy hết quy trình của viện):
-- làm thay Quản lý viện phần cài đặt ban đầu mà Phụ lục 25 ghi "Viện nhập khi cài đặt" hoặc "Theo cơ sở" (V000_04 để
-- null, đọc khi chưa đặt thì lỗi):
--   CFG-M11-10 tài khoản thu (VietQR, sao kê), CFG-M11-11 thông tin viện (chứng từ, phiếu chuyển viện, màn gọi 115),
--   CFG-M03-01 sức chứa khu nghỉ bán trú, CFG-M09-01 ngưỡng tỷ lệ phục vụ theo mẫu ca, CFG-M09-12 trọng số theo mức chăm
--   sóc, CFG-M08-04 giờ bữa dự kiến (kèm danh mục bữa ăn M08.MEAL: khóa của mealTimes là mã bữa);
-- và danh mục lý do chuyển giường (003 FR-010; chỉ có "Kiểm soát lây nhiễm" cố định nên lệnh chuyển giường thường không
-- chọn được lý do). Đổi tham số ghi như ParameterStore: cập nhật current_value, tăng version, thêm dòng lịch sử
-- M15.PARAMETER (command CHANGE, người thực hiện demo.quanly, có lý do). Cú pháp dùng được ở MySQL 8.0 và 8.4.

SET @demo_at = UTC_TIMESTAMP(6);
SET @manager = (SELECT id FROM account WHERE username = 'demo.quanly');
SET @reason = 'Cài đặt ban đầu của viện (dữ liệu demo)';

-- 011: bữa ăn (MealAttributes: servesResidential, servesDayCare)
INSERT INTO catalog_item (catalog_type, module, code, name, sort_order, status, attributes) VALUES
    ('M08.MEAL', 'M08', 'BREAKFAST', 'Bữa sáng', 1, 'ACTIVE',
     JSON_OBJECT('servesResidential', TRUE, 'servesDayCare', FALSE)),
    ('M08.MEAL', 'M08', 'LUNCH', 'Bữa trưa', 2, 'ACTIVE',
     JSON_OBJECT('servesResidential', TRUE, 'servesDayCare', TRUE)),
    ('M08.MEAL', 'M08', 'AFTERNOON_SNACK', 'Bữa phụ chiều', 3, 'ACTIVE',
     JSON_OBJECT('servesResidential', TRUE, 'servesDayCare', TRUE)),
    ('M08.MEAL', 'M08', 'DINNER', 'Bữa tối', 4, 'ACTIVE',
     JSON_OBJECT('servesResidential', TRUE, 'servesDayCare', FALSE));

-- 003: lý do chuyển giường (catalog_item M03.BED_TRANSFER_REASON từ V099_03; nhóm y tế, an toàn: medicalSafety, FR-030
-- không cần yêu cầu điều chỉnh khi khác giá)
INSERT INTO catalog_item (catalog_type, module, code, name, sort_order, status, attributes) VALUES
    ('M03.BED_TRANSFER_REASON', 'M03', 'FAMILY_REQUEST', 'Theo nguyện vọng gia đình', 0, 'ACTIVE',
     JSON_OBJECT('medicalSafety', CAST('false' AS JSON), 'fixed', CAST('false' AS JSON))),
    ('M03.BED_TRANSFER_REASON', 'M03', 'ROOMMATE_CONFLICT', 'Không hợp người cùng phòng', 0, 'ACTIVE',
     JSON_OBJECT('medicalSafety', CAST('false' AS JSON), 'fixed', CAST('false' AS JSON))),
    ('M03.BED_TRANSFER_REASON', 'M03', 'ROOM_MAINTENANCE', 'Sửa chữa, bảo trì phòng', 0, 'ACTIVE',
     JSON_OBJECT('medicalSafety', CAST('false' AS JSON), 'fixed', CAST('false' AS JSON))),
    ('M03.BED_TRANSFER_REASON', 'M03', 'CARE_LEVEL_CHANGE', 'Đổi mức chăm sóc', 0, 'ACTIVE',
     JSON_OBJECT('medicalSafety', CAST('false' AS JSON), 'fixed', CAST('false' AS JSON))),
    ('M03.BED_TRANSFER_REASON', 'M03', 'CLOSE_MONITORING', 'Cần theo dõi gần phòng trực', 0, 'ACTIVE',
     JSON_OBJECT('medicalSafety', CAST('true' AS JSON), 'fixed', CAST('false' AS JSON))),
    ('M03.BED_TRANSFER_REASON', 'M03', 'FALL_RISK', 'Nguy cơ ngã cao', 0, 'ACTIVE',
     JSON_OBJECT('medicalSafety', CAST('true' AS JSON), 'fixed', CAST('false' AS JSON)));

-- 000: giá trị tham số
SET @night_template = (SELECT id FROM catalog_item
                       WHERE catalog_type = 'M09.SHIFT_TEMPLATE' AND name = 'Ca đêm' ORDER BY id LIMIT 1);

CREATE TEMPORARY TABLE demo_parameter (code VARCHAR(16) PRIMARY KEY, value JSON NOT NULL);
INSERT INTO demo_parameter (code, value) VALUES
    ('CFG-M11-10', JSON_OBJECT('bin', '970436', 'accountNumber', '0011000999999',
                               'accountHolder', 'VIEN DUONG LAO AN KHANG')),
    ('CFG-M11-11', JSON_OBJECT('name', 'Viện dưỡng lão An Khang (demo)',
                               'address', '12 Đường Láng, Quận Đống Đa, Hà Nội',
                               'taxCode', '0109999999', 'phone', '02400000000', 'logoFileId', NULL)),
    ('CFG-M03-01', CAST('12' AS JSON)),
    ('CFG-M09-01', JSON_OBJECT('9001', 6, CAST(@night_template AS CHAR), 10)),
    ('CFG-M09-12', JSON_OBJECT('BASIC', 1, 'REGULAR', 1.5, 'SPECIAL', 2.5)),
    ('CFG-M08-04', JSON_OBJECT('mealTimes', JSON_OBJECT('BREAKFAST', '06:30', 'LUNCH', '11:00',
                                                        'AFTERNOON_SNACK', '15:00', 'DINNER', '17:30'),
                               'lateDeliveryThreshold', JSON_OBJECT('value', 30, 'unit', 'MINUTES')));

UPDATE parameter p JOIN demo_parameter d ON d.code = p.code
SET p.current_value = d.value, p.version = p.version + 1, p.changed_at = @demo_at, p.changed_by = @manager;

INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, starts_at, command, actor_id, by_system,
                            reason, details, unique_key)
SELECT 'M15.PARAMETER', 0, d.code, 'M15', 'POINT', @demo_at, 'CHANGE', @manager, FALSE, @reason,
       JSON_OBJECT('value', d.value),
       CONCAT('M15.PARAMETER:', d.code, ':', DATE_FORMAT(@demo_at, '%Y-%m-%dT%H:%i:%s.%f'))
FROM demo_parameter d;

DROP TEMPORARY TABLE demo_parameter;
