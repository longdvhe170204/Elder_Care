-- T005, FR-042: chỉ nạp mức mặc định đã có căn cứ nghiệp vụ.
-- Ngã: mục 9.3; đi lạc/không trở về: mục 8.9, BR-M04-17.
-- R-15: thêm dòng vào catalog_item (DataGroup=CATALOG), không tạo bảng riêng.
INSERT INTO catalog_item (catalog_type, module, code, name, sort_order, status, attributes)
VALUES
    ('M05.INCIDENT_TYPE', 'M05', 'FALL', 'Ngã', 1, 'ACTIVE',
     JSON_OBJECT('defaultLevel', 'CRITICAL', 'nonHealth', FALSE, 'allowsNoResident', FALSE)),
    ('M05.INCIDENT_TYPE', 'M05', 'MISSING_OR_NOT_RETURNED', 'Đi lạc hoặc không trở về', 7, 'ACTIVE',
     JSON_OBJECT('defaultLevel', 'CRITICAL', 'nonHealth', FALSE, 'allowsNoResident', FALSE));
