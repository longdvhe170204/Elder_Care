-- Chỉ Bác sĩ lập và sửa kế hoạch chăm sóc. Điều dưỡng vẫn xem trong phạm vi phân công.
DELETE FROM permission_matrix
WHERE role = 'AC-04' AND function_code = 'CARE_PLAN' AND operation IN ('CREATE', 'UPDATE');

INSERT INTO permission_matrix (role, function_code, operation, scope, footnote_codes, source)
VALUES ('AC-03', 'CARE_PLAN', 'CREATE', 'FACILITY', '[]', 'APPENDIX_27'),
       ('AC-03', 'CARE_PLAN', 'UPDATE', 'FACILITY', '[]', 'APPENDIX_27');
