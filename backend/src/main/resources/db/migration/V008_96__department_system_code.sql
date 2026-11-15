-- 019 ghi tăng tài sản giường kèm lệnh Tạo giường (Q-238, FR-011) cần biết bộ phận Chăm sóc; chủ dự án duyệt 2026-11-11
-- mã hệ thống CARE cho bộ phận Chăm sóc (catalog_item M09.DEPARTMENT, cột code sẵn có, duy nhất theo code_key).
UPDATE catalog_item
SET code = 'CARE'
WHERE catalog_type = 'M09.DEPARTMENT'
  AND name = 'Chăm sóc'
  AND status = 'ACTIVE'
  AND code IS NULL;
