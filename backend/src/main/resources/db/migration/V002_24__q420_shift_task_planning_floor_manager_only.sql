-- 2026-11-23 (Q-420, chủ dự án chốt): chỉ Trưởng tầng lập công việc của ca. Phụ lục 27 dòng "Lập công việc tháng, công
-- việc phát sinh" ô Điều dưỡng đổi T⁴¹ → P⁴¹: Điều dưỡng chỉ xem công việc của ca trong phạm vi phân công; công việc
-- phát sinh vẫn tạo qua dòng "Checklist" (TASK_CHECKLIST), không qua dòng này. Đối chiếu bằng
-- PermissionMatrixMatchesSourceIT. Cú pháp dùng được ở MySQL 8.0 và 8.4 (research R-01 của 000).
DELETE FROM permission_matrix
WHERE role = 'AC-04' AND function_code = 'SHIFT_TASK_PLANNING' AND operation IN ('CREATE', 'UPDATE');
