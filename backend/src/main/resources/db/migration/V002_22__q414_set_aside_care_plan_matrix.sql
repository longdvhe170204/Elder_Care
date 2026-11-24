-- 2026-11-22: Q-414 (Bác sĩ lập và ban hành kế hoạch, bỏ duyệt) gác lại vì thuộc luồng tiếp nhận BF-01. Trả dòng
-- "Kế hoạch chăm sóc" của Phụ lục 27 về QL X, TT X, BS D, ĐD T, CS P như V002_04 (V002_21 đã đổi). Các dòng khác của
-- V002_21 (Lập công việc tháng, Mốc sinh hoạt cố định, Lịch chăm sóc hằng ngày) giữ nguyên. Đối chiếu bằng
-- PermissionMatrixMatchesSourceIT. Cú pháp dùng được ở MySQL 8.0 và 8.4 (research R-01 của 000).

DELETE FROM permission_matrix WHERE function_code = 'CARE_PLAN' AND role IN ('AC-03', 'AC-04');

INSERT INTO permission_matrix (role, function_code, operation, scope, footnote_codes, source)
VALUES ('AC-03', 'CARE_PLAN', 'VIEW', 'FACILITY', '[]', 'APPENDIX_27'),
       ('AC-03', 'CARE_PLAN', 'APPROVE', 'FACILITY', '[]', 'APPENDIX_27'),
       ('AC-04', 'CARE_PLAN', 'VIEW', 'ASSIGNED', '[]', 'APPENDIX_27'),
       ('AC-04', 'CARE_PLAN', 'CREATE', 'ASSIGNED', '[]', 'APPENDIX_27'),
       ('AC-04', 'CARE_PLAN', 'UPDATE', 'ASSIGNED', '[]', 'APPENDIX_27');
