-- Q-396 (2026-10-29): Phụ lục 27 bỏ dòng "Bản tin định kỳ" (UC-60); hệ thống không còn bản tin.
-- Q-394 (2026-10-29): dòng "Danh sách chờ, dấu ưu tiên" ô Quản lý viện đổi D thành X (bỏ duyệt điều chỉnh điểm).
-- Xóa quyền đã nạp ở V002_04 và mọi thu hẹp quyền trỏ tới các quyền này, như V002_10. Đối chiếu bằng
-- PermissionMatrixMatchesSourceIT. Cú pháp dùng được ở MySQL 8.0 và 8.4 (research R-01 của 000).

DELETE FROM permission_restriction WHERE function_code = 'PERIODIC_NEWSLETTER';

DELETE FROM permission_matrix WHERE function_code = 'PERIODIC_NEWSLETTER';

DELETE FROM permission_restriction WHERE function_code = 'WAITING_LIST' AND operation = 'APPROVE';

DELETE FROM permission_matrix WHERE function_code = 'WAITING_LIST' AND operation = 'APPROVE';
