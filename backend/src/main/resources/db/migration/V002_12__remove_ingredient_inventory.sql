-- Q-392 (2026-10-29): Phụ lục 27 bỏ dòng "Kho nguyên liệu" (UC-91, UC-92); hệ thống không quản lý kho nguyên liệu.
-- Xóa quyền đã nạp ở V002_04 và mọi thu hẹp quyền trỏ tới chức năng này, như V002_10. Đối chiếu bằng
-- PermissionMatrixMatchesSourceIT. Cú pháp dùng được ở MySQL 8.0 và 8.4 (research R-01 của 000).

DELETE FROM permission_restriction WHERE function_code = 'INGREDIENT_INVENTORY';

DELETE FROM permission_matrix WHERE function_code = 'INGREDIENT_INVENTORY';
