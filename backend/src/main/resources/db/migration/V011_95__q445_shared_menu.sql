-- 011 T047 (Q-445, 2026-11-24; data-model mục 13): thực đơn chung cả viện, Quản lý viện duyệt.
-- (1) Món: bỏ bước xác nhận và phiên bản; món Chờ xác nhận về Nháp; ghi chú thành mô tả.
-- (2) Chế độ ăn: bỏ món an toàn.
-- (3) Phụ lục 27 dòng "Món ăn, thực đơn": Quản lý viện D, Bác sĩ X; Trưởng tầng, Nhân viên chăm sóc, Hành chính,
--     Kế toán X (quy đổi ô như V002_04; PermissionMatrixMatchesSourceIT đối chiếu từng ô).
-- Nội dung thực đơn cũ (theo chế độ ăn) đọc được vì MenuContent bỏ trường thừa và gộp món trùng (data-model 13.6).
-- Cú pháp dùng được ở MySQL 8.0 và 8.4 (research R-01 của 000).

UPDATE catalog_item
SET attributes = JSON_SET(attributes, '$.status', 'DRAFT')
WHERE catalog_type = 'M08.DISH'
  AND attributes ->> '$.status' = 'PENDING_CONFIRMATION';

UPDATE catalog_item
SET attributes = JSON_REMOVE(
        JSON_SET(attributes, '$.description', attributes -> '$.note'),
        '$.note', '$.effectiveVersionId')
WHERE catalog_type = 'M08.DISH'
  AND JSON_CONTAINS_PATH(attributes, 'one', '$.note');

UPDATE catalog_item
SET attributes = JSON_REMOVE(attributes, '$.effectiveVersionId')
WHERE catalog_type = 'M08.DISH'
  AND JSON_CONTAINS_PATH(attributes, 'one', '$.effectiveVersionId');

UPDATE catalog_item
SET attributes = JSON_REMOVE(attributes, '$.safeDishId')
WHERE catalog_type = 'M08.DIET'
  AND JSON_CONTAINS_PATH(attributes, 'one', '$.safeDishId');

DELETE FROM permission_matrix
WHERE role = 'AC-03'
  AND function_code = 'DISH_MENU'
  AND operation = 'APPROVE'
  AND source = 'APPENDIX_27';

INSERT INTO permission_matrix (role, function_code, operation, scope, footnote_codes, source)
VALUES ('AC-01', 'DISH_MENU', 'APPROVE', 'FACILITY', '[]', 'APPENDIX_27'),
       ('AC-02', 'DISH_MENU', 'VIEW', 'FACILITY', '[]', 'APPENDIX_27'),
       ('AC-05', 'DISH_MENU', 'VIEW', 'FACILITY', '[]', 'APPENDIX_27'),
       ('AC-09', 'DISH_MENU', 'VIEW', 'FACILITY', '[]', 'APPENDIX_27'),
       ('AC-14', 'DISH_MENU', 'VIEW', 'FACILITY', '[]', 'APPENDIX_27');
