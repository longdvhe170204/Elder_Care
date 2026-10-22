-- Q-385 (2026-10-22): Phụ lục 27 bỏ dòng "Chốt quỹ ngày" (UC-93); quỹ tiền mặt thuộc phần mềm kế toán bên ngoài.
-- Xóa quyền đã nạp ở V002_04 và mọi thu hẹp quyền trỏ tới chức năng này. Đối chiếu bằng PermissionMatrixMatchesSourceIT.
-- Cú pháp dùng được ở MySQL 8.0 và 8.4 (research R-01 của 000).

DELETE FROM permission_restriction WHERE function_code = 'DAILY_CASH_CLOSING';

DELETE FROM permission_matrix WHERE function_code = 'DAILY_CASH_CLOSING';
