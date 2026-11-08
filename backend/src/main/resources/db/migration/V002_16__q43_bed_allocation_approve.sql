-- Q-43 (chủ dự án duyệt 2026-11-07): Quản lý viện duyệt yêu cầu nhập bù phân bổ giường lùi quá CFG-M03-07 (003 FR-027 (b));
-- Phụ lục 27 dòng "Phân bổ, chuyển giường" ô Quản lý viện từ X thành "X, D". Quy đổi ô như V002_04.
-- PermissionMatrixMatchesSourceIT đối chiếu từng ô. Cú pháp dùng được ở MySQL 8.0 và 8.4 (research R-01 của 000).

INSERT INTO permission_matrix (role, function_code, operation, scope, footnote_codes, source)
VALUES ('AC-01', 'BED_ALLOCATION', 'APPROVE', 'FACILITY', '[]', 'APPENDIX_27');
