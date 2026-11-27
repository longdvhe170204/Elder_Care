-- 011 T058 (Q-445, 2026-11-24): bỏ dòng "Xác nhận phục vụ suất đặc biệt" (UC-77) khỏi Phụ lục 27; không còn xác nhận phục
-- vụ, nhân viên đối chiếu dị ứng, chế độ ăn trong hồ sơ khi phục vụ (BR-M08-14). PermissionMatrixMatchesSourceIT đối chiếu
-- từng ô. Cú pháp dùng được ở MySQL 8.0 và 8.4 (research R-01 của 000).

DELETE FROM permission_matrix
WHERE function_code = 'SPECIAL_MEAL_SERVICE'
  AND source = 'APPENDIX_27';
