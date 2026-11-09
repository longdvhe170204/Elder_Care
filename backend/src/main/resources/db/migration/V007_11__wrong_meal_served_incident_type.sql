-- Loại sự cố "Phục vụ sai suất ăn hoặc chế độ ăn" (nghiep-vu 9.3, Q-398; 011 xác nhận phục vụ "đã ăn", BR-M08-14).
-- R-15: dòng danh mục chung, không tạo bảng riêng.
INSERT INTO catalog_item (catalog_type, module, code, name, sort_order, status, attributes)
VALUES
    ('M05.INCIDENT_TYPE', 'M05', 'WRONG_MEAL_SERVED', 'Phục vụ sai suất ăn hoặc chế độ ăn', 4, 'ACTIVE',
     JSON_OBJECT('defaultLevel', 'MEDIUM', 'nonHealth', FALSE, 'allowsNoResident', FALSE));
