-- 007 T005 (phần đã có giá trị khởi tạo trong FR-039, FR-048a, BR-M05-20).
-- R-15: danh mục dùng catalog_item (DataGroup=CATALOG, V000_13), không tạo bảng riêng.
-- Trạng thái ACTIVE/INACTIVE của quy tắc thể hiện bật/tắt; mức và mã CFG nằm ở attributes.
-- Không nạp ngưỡng vật lý, đơn vị, nhóm sinh hiệu hoặc mức sự cố chưa được chủ dự án chốt.
-- Trạng thái theo người, checkpoint và khóa chỉ được tạo qua API bảng chung khi có lệnh/job tương ứng.

INSERT INTO catalog_item (catalog_type, module, code, name, sort_order, status, attributes)
VALUES
    ('M05.TREND_RULE', 'M05', 'WEIGHT_LOSS', 'Sụt cân', 1, 'ACTIVE',
     JSON_OBJECT('level', 'MEDIUM', 'parameterCode', 'CFG-M05-03')),
    ('M05.TREND_RULE', 'M05', 'MISSED_MEASUREMENT', 'Bỏ lỡ lần đo theo lịch', 2, 'ACTIVE',
     JSON_OBJECT('level', 'LOW', 'parameterCode', NULL)),
    ('M05.TREND_RULE', 'M05', 'PROLONGED_INSOMNIA', 'Mất ngủ kéo dài', 3, 'ACTIVE',
     JSON_OBJECT('level', 'LOW', 'parameterCode', 'CFG-M05-05')),
    ('M05.TREND_RULE', 'M05', 'MEDICATION_REFUSAL', 'Từ chối cùng một thuốc', 4, 'ACTIVE',
     JSON_OBJECT('level', 'MEDIUM', 'parameterCode', 'CFG-M05-04'));

INSERT INTO catalog_item (catalog_type, module, code, name, sort_order, status, attributes)
VALUES
    ('M05.PROGRESSION_TYPE', 'M05', 'CHEST_COMPRESSION', 'Ép tim ngoài lồng ngực', 1, 'ACTIVE',
     JSON_OBJECT('group', 'ACTION', 'resuscitation', TRUE)),
    ('M05.PROGRESSION_TYPE', 'M05', 'VENTILATION_SUPPORT', 'Hỗ trợ thông khí', 2, 'ACTIVE',
     JSON_OBJECT('group', 'ACTION', 'resuscitation', TRUE)),
    ('M05.PROGRESSION_TYPE', 'M05', 'DEFIBRILLATION', 'Sốc điện', 3, 'ACTIVE',
     JSON_OBJECT('group', 'ACTION', 'resuscitation', TRUE)),
    ('M05.PROGRESSION_TYPE', 'M05', 'INTUBATION', 'Đặt nội khí quản', 4, 'ACTIVE',
     JSON_OBJECT('group', 'ACTION', 'resuscitation', TRUE)),
    ('M05.PROGRESSION_TYPE', 'M05', 'EMERGENCY_MEDICATION', 'Dùng thuốc cấp cứu', 5, 'ACTIVE',
     JSON_OBJECT('group', 'ACTION', 'resuscitation', TRUE)),
    ('M05.PROGRESSION_TYPE', 'M05', 'CALL_SUPPORT', 'Gọi hỗ trợ', 6, 'ACTIVE',
     JSON_OBJECT('group', 'NOTIFICATION', 'resuscitation', FALSE)),
    ('M05.PROGRESSION_TYPE', 'M05', 'CALL_EMERGENCY', 'Gọi cấp cứu', 7, 'ACTIVE',
     JSON_OBJECT('group', 'NOTIFICATION', 'resuscitation', FALSE)),
    ('M05.PROGRESSION_TYPE', 'M05', 'REPORT_HEALTH_AUTHORITY', 'Báo cơ quan y tế', 8, 'ACTIVE',
     JSON_OBJECT('group', 'NOTIFICATION', 'resuscitation', FALSE)),
    ('M05.PROGRESSION_TYPE', 'M05', 'SOURCE_RESOLVED', 'Nguồn đã xử lý', 9, 'ACTIVE',
     JSON_OBJECT('group', 'SYSTEM', 'resuscitation', FALSE)),
    ('M05.PROGRESSION_TYPE', 'M05', 'TRANSFER_REFUSED', 'Chuyển viện bị từ chối', 10, 'ACTIVE',
     JSON_OBJECT('group', 'SYSTEM', 'resuscitation', FALSE)),
    ('M05.PROGRESSION_TYPE', 'M05', 'FOUND', 'Đã tìm thấy', 11, 'ACTIVE',
     JSON_OBJECT('group', 'SYSTEM', 'resuscitation', FALSE)),
    ('M05.PROGRESSION_TYPE', 'M05', 'CANCELLATION_REQUESTED', 'Đề nghị hủy', 12, 'ACTIVE',
     JSON_OBJECT('group', 'SYSTEM', 'resuscitation', FALSE));
