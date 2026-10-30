-- Q-391 (2026-10-27): Phụ lục 27 thêm dòng "Danh mục công việc chăm sóc" (loại công việc, giá trị kết quả, loại lý do
-- Không thực hiện; feature 005 FR-038, FR-028a). Quy đổi ô như V002_04: C → VIEW, CREATE, UPDATE; X → VIEW; danh mục
-- không gắn tầng nên phạm vi FACILITY. Đối chiếu bằng PermissionMatrixMatchesSourceIT.
-- Cú pháp dùng được ở MySQL 8.0 và 8.4 (research R-01 của 000).

INSERT INTO permission_matrix (role, function_code, operation, scope, footnote_codes, source)
VALUES ('AC-01', 'CARE_TASK_CATALOG', 'VIEW', 'FACILITY', '[]', 'APPENDIX_27'),
       ('AC-01', 'CARE_TASK_CATALOG', 'CREATE', 'FACILITY', '[]', 'APPENDIX_27'),
       ('AC-01', 'CARE_TASK_CATALOG', 'UPDATE', 'FACILITY', '[]', 'APPENDIX_27'),
       ('AC-02', 'CARE_TASK_CATALOG', 'VIEW', 'FACILITY', '[]', 'APPENDIX_27'),
       ('AC-03', 'CARE_TASK_CATALOG', 'VIEW', 'FACILITY', '[]', 'APPENDIX_27'),
       ('AC-04', 'CARE_TASK_CATALOG', 'VIEW', 'FACILITY', '[]', 'APPENDIX_27'),
       ('AC-05', 'CARE_TASK_CATALOG', 'VIEW', 'FACILITY', '[]', 'APPENDIX_27');
