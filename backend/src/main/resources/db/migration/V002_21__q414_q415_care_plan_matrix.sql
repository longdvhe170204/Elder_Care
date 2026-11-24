-- Q-414, Q-415 (chủ dự án chốt 2026-11-22, luồng chăm sóc hằng ngày): Phụ lục 27 dòng "Kế hoạch chăm sóc" đổi thành
-- QL X, TT X, BS T⁵³, ĐD X, CS P (Bác sĩ lập và ban hành, không qua duyệt; Điều dưỡng chỉ xem) và dòng "Lập công việc
-- của ca" đổi tên thành "Lập công việc tháng, công việc phát sinh", ô Trưởng tầng T⁵³. Quy đổi ô như V002_04: T → VIEW,
-- CREATE, UPDATE theo phạm vi của vai trò (Bác sĩ toàn viện); X → VIEW toàn viện. Đối chiếu bằng
-- PermissionMatrixMatchesSourceIT. Cú pháp dùng được ở MySQL 8.0 và 8.4 (research R-01 của 000).

DELETE FROM permission_matrix WHERE function_code = 'CARE_PLAN' AND role IN ('AC-03', 'AC-04');

INSERT INTO permission_matrix (role, function_code, operation, scope, footnote_codes, source)
VALUES ('AC-03', 'CARE_PLAN', 'VIEW', 'FACILITY', '["53"]', 'APPENDIX_27'),
       ('AC-03', 'CARE_PLAN', 'CREATE', 'FACILITY', '["53"]', 'APPENDIX_27'),
       ('AC-03', 'CARE_PLAN', 'UPDATE', 'FACILITY', '["53"]', 'APPENDIX_27'),
       ('AC-04', 'CARE_PLAN', 'VIEW', 'FACILITY', '[]', 'APPENDIX_27');

UPDATE permission_matrix SET footnote_codes = '["53"]'
WHERE role = 'AC-02' AND function_code = 'SHIFT_TASK_PLANNING';

-- Q-416: hai dòng mới của Phụ lục 27. "Mốc sinh hoạt cố định": QL T, TT X, BS X, ĐD X, CS X (005 FR-056).
-- "Lịch chăm sóc hằng ngày": QL X, TT X, BS X, ĐD X, CS P, NT X⁵⁴ (theo quan hệ), NCT X⁴⁵ (chính mình) (005 FR-055).
INSERT INTO permission_matrix (role, function_code, operation, scope, footnote_codes, source)
VALUES ('AC-01', 'DAILY_ROUTINE', 'VIEW', 'FACILITY', '[]', 'APPENDIX_27'),
       ('AC-01', 'DAILY_ROUTINE', 'CREATE', 'FACILITY', '[]', 'APPENDIX_27'),
       ('AC-01', 'DAILY_ROUTINE', 'UPDATE', 'FACILITY', '[]', 'APPENDIX_27'),
       ('AC-02', 'DAILY_ROUTINE', 'VIEW', 'FACILITY', '[]', 'APPENDIX_27'),
       ('AC-03', 'DAILY_ROUTINE', 'VIEW', 'FACILITY', '[]', 'APPENDIX_27'),
       ('AC-04', 'DAILY_ROUTINE', 'VIEW', 'FACILITY', '[]', 'APPENDIX_27'),
       ('AC-05', 'DAILY_ROUTINE', 'VIEW', 'FACILITY', '[]', 'APPENDIX_27'),
       ('AC-01', 'DAILY_CARE_SCHEDULE', 'VIEW', 'FACILITY', '[]', 'APPENDIX_27'),
       ('AC-02', 'DAILY_CARE_SCHEDULE', 'VIEW', 'FACILITY', '[]', 'APPENDIX_27'),
       ('AC-03', 'DAILY_CARE_SCHEDULE', 'VIEW', 'FACILITY', '[]', 'APPENDIX_27'),
       ('AC-04', 'DAILY_CARE_SCHEDULE', 'VIEW', 'FACILITY', '[]', 'APPENDIX_27'),
       ('AC-05', 'DAILY_CARE_SCHEDULE', 'VIEW', 'ASSIGNED', '[]', 'APPENDIX_27'),
       ('AC-10', 'DAILY_CARE_SCHEDULE', 'VIEW', 'RELATIONSHIP', '["54"]', 'APPENDIX_27'),
       ('AC-16', 'DAILY_CARE_SCHEDULE', 'VIEW', 'SELF', '["45"]', 'APPENDIX_27');
