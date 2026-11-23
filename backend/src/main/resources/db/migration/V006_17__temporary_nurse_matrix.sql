-- BF-07 (Q-436, BR-M07-31): Phụ lục 27 thêm dòng "Chỉ định Điều dưỡng phụ trách tạm" (UC-41), chỉ Quản lý viện có ô T.
-- Quy đổi ô như V002_04: T của Quản lý viện (phạm vi cả viện) thành xem, tạo, sửa; dòng không có chú thích.
-- PermissionMatrixMatchesSourceIT đối chiếu từng ô. Cú pháp dùng được ở MySQL 8.0 và 8.4 (research R-01 của 000).

INSERT INTO permission_matrix (role, function_code, operation, scope, footnote_codes, source)
VALUES ('AC-01', 'TEMPORARY_NURSE_ASSIGNMENT', 'VIEW', 'FACILITY', '[]', 'APPENDIX_27'),
       ('AC-01', 'TEMPORARY_NURSE_ASSIGNMENT', 'CREATE', 'FACILITY', '[]', 'APPENDIX_27'),
       ('AC-01', 'TEMPORARY_NURSE_ASSIGNMENT', 'UPDATE', 'FACILITY', '[]', 'APPENDIX_27');
