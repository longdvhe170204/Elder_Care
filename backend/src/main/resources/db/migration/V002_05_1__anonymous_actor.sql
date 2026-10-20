-- 002 T041: người thực hiện "chưa đăng nhập" cho lần đăng nhập thất bại (FR-015; research A-03; chủ dự án duyệt
-- 2026-10-20). Thêm giá trị cuối ENUM nên MySQL đổi tại chỗ, không chép lại bảng. Số V002_05_1 vì V002_06 → 08 đã dành
-- cho task sau (T055, T063, T090).
ALTER TABLE audit_log
    MODIFY actor_type ENUM ('USER', 'SCHEDULER', 'BOOTSTRAP', 'ANONYMOUS') NOT NULL;
