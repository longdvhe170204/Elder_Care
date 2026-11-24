-- 2026-11-22 (Q-416): màn "Lịch chăm sóc hằng ngày" chỉ dành cho người thân và người cao tuổi; nhân viên xem việc của mình
-- qua checklist, lịch làm việc. Phụ lục 27 dòng "Lịch chăm sóc hằng ngày" bỏ ô QL, TT, BS, ĐD, CS (V002_21 đã nạp).
-- Đối chiếu bằng PermissionMatrixMatchesSourceIT. Cú pháp dùng được ở MySQL 8.0 và 8.4 (research R-01 của 000).
DELETE FROM permission_matrix
WHERE function_code = 'DAILY_CARE_SCHEDULE' AND role IN ('AC-01', 'AC-02', 'AC-03', 'AC-04', 'AC-05');
