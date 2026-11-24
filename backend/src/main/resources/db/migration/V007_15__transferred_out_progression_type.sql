-- Diễn biến hệ thống "Đã chuyển viện": lệnh Chuyển viện thành công ghi một dòng vào sự cố để người xử lý thấy sự cố đã có
-- bước chuyển (FR-049); sự cố vẫn Đang xử lý cho tới khi người có quyền đóng kèm kết quả (BR-M05-09).
INSERT INTO catalog_item (catalog_type, module, code, name, sort_order, status, attributes)
VALUES
    ('M05.PROGRESSION_TYPE', 'M05', 'TRANSFERRED_OUT', 'Đã chuyển viện', 13, 'ACTIVE',
     JSON_OBJECT('group', 'SYSTEM', 'resuscitation', FALSE));
