-- Phụ lục 27 dòng "Danh mục dị nguyên, mức độ dị ứng, nguồn thông tin, nhu cầu chăm sóc" cột Hành chính = X⁵⁰ (chủ dự án
-- 2026-11-20): Hành chính xem các mục Hiệu lực để chọn khi ghi tự khai của gia đình (Q-400, BR-M01-17, V002_14); trước đó
-- ô này trống nên form thêm mục dị ứng của Hành chính bị 403 (BF-01 bước 3). Chỉ mục Hiệu lực do module resident kiểm
-- (CatalogService); Hành chính không tạo, sửa danh mục. PermissionMatrixMatchesSourceIT đối chiếu từng ô với Phụ lục 27.
INSERT INTO permission_matrix (role, function_code, operation, scope, footnote_codes, source)
VALUES ('AC-09', 'HEALTH_CATALOGS', 'VIEW', 'FACILITY', '["50"]', 'APPENDIX_27');
