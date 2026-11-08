-- 007 T005: danh mục khởi tạo đã chốt ở Q-398 (nghiep-vu mục 9.3 và 10.1). Không nạp loại, chỉ số hay ngưỡng ngoài hai bảng này (Q-306).
-- R-15: dùng catalog_item (DataGroup=CATALOG, V000_13). Mã dịch sang tiếng Anh theo glossary mục 13 (chủ dự án duyệt 2026-11-08).
-- Fall, MISSING_OR_NOT_RETURNED ở V007_02_1; WRONG_MEAL_SERVED ở V007_11.

INSERT INTO catalog_item (catalog_type, module, code, name, sort_order, status, attributes)
VALUES
    ('M06.VITAL_SIGN', 'M06', 'BLOOD_PRESSURE', 'Huyết áp', 1, 'ACTIVE',
     JSON_OBJECT('unit', 'mmHg', 'vitalSign', TRUE)),
    ('M06.VITAL_SIGN', 'M06', 'HEART_RATE', 'Nhịp tim', 2, 'ACTIVE',
     JSON_OBJECT('unit', 'nhịp/phút', 'vitalSign', TRUE)),
    ('M06.VITAL_SIGN', 'M06', 'BODY_TEMPERATURE', 'Nhiệt độ cơ thể', 3, 'ACTIVE',
     JSON_OBJECT('unit', '°C', 'vitalSign', TRUE)),
    ('M06.VITAL_SIGN', 'M06', 'SPO2', 'Độ bão hòa oxy ngoại vi', 4, 'ACTIVE',
     JSON_OBJECT('unit', '%', 'vitalSign', TRUE)),
    ('M06.VITAL_SIGN', 'M06', 'BLOOD_GLUCOSE', 'Đường huyết', 5, 'ACTIVE',
     JSON_OBJECT('unit', 'mmol/L', 'vitalSign', FALSE)),
    ('M06.VITAL_SIGN', 'M06', 'WEIGHT', 'Cân nặng', 6, 'ACTIVE',
     JSON_OBJECT('unit', 'kg', 'vitalSign', FALSE));

INSERT INTO catalog_item (catalog_type, module, code, name, parent_id, sort_order, status, attributes)
SELECT 'M06.VITAL_SIGN_COMPONENT', 'M06', c.code, c.name, p.id, c.sort_order, 'ACTIVE',
       JSON_OBJECT('decimalPlaces', c.decimal_places)
FROM (SELECT 'BLOOD_PRESSURE' parent_code, 'SYSTOLIC' code, 'Tâm thu' name, 0 sort_order, 0 decimal_places
      UNION ALL SELECT 'BLOOD_PRESSURE', 'DIASTOLIC', 'Tâm trương', 1, 0
      UNION ALL SELECT 'HEART_RATE', 'VALUE', 'Giá trị', 0, 0
      UNION ALL SELECT 'BODY_TEMPERATURE', 'VALUE', 'Giá trị', 0, 1
      UNION ALL SELECT 'SPO2', 'VALUE', 'Giá trị', 0, 0
      UNION ALL SELECT 'BLOOD_GLUCOSE', 'VALUE', 'Giá trị', 0, 1
      UNION ALL SELECT 'WEIGHT', 'VALUE', 'Giá trị', 0, 1) c
         JOIN catalog_item p ON p.catalog_type = 'M06.VITAL_SIGN' AND p.code = c.parent_code;

INSERT INTO catalog_item (catalog_type, module, code, name, sort_order, status, attributes)
VALUES
    ('M05.INCIDENT_TYPE', 'M05', 'BREATHING_OR_CONSCIOUSNESS', 'Khó thở cấp hoặc mất ý thức', 2, 'ACTIVE',
     JSON_OBJECT('defaultLevel', 'CRITICAL', 'nonHealth', FALSE, 'allowsNoResident', FALSE)),
    ('M05.INCIDENT_TYPE', 'M05', 'MEDICATION', 'Sự cố liên quan đến sử dụng thuốc', 3, 'ACTIVE',
     JSON_OBJECT('defaultLevel', 'MEDIUM', 'nonHealth', FALSE, 'allowsNoResident', FALSE)),
    ('M05.INCIDENT_TYPE', 'M05', 'CHOKING_OR_AIRWAY_OBSTRUCTION', 'Sặc/nghẹn thức ăn, nghi tắc đường thở', 5, 'ACTIVE',
     JSON_OBJECT('defaultLevel', 'CRITICAL', 'nonHealth', FALSE, 'allowsNoResident', FALSE)),
    ('M05.INCIDENT_TYPE', 'M05', 'BEHAVIOR', 'Kích động, gây gổ liên quan đến người cao tuổi', 6, 'ACTIVE',
     JSON_OBJECT('defaultLevel', 'MEDIUM', 'nonHealth', FALSE, 'allowsNoResident', FALSE)),
    ('M05.INCIDENT_TYPE', 'M05', 'OTHER_INJURY', 'Chấn thương khác ở người cao tuổi', 8, 'ACTIVE',
     JSON_OBJECT('defaultLevel', 'MEDIUM', 'nonHealth', FALSE, 'allowsNoResident', FALSE)),
    ('M05.INCIDENT_TYPE', 'M05', 'OTHER_HEALTH_CARE', 'Sự cố sức khỏe/chăm sóc khác của người cao tuổi', 9, 'ACTIVE',
     JSON_OBJECT('defaultLevel', 'MEDIUM', 'nonHealth', FALSE, 'allowsNoResident', FALSE));
