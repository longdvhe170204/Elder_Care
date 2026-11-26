-- Q-448 (chủ dự án chốt 2026-11-26, test luồng BF-01): dòng mới Phụ lục 27 "Danh sách liên hệ người thân" (UC-55):
-- Quản lý viện, Trưởng tầng, Bác sĩ, Điều dưỡng, Hành chính X. Quy đổi ô như V002_04: X → VIEW toàn viện. Danh sách chỉ
-- gồm người thân có quan hệ Hiệu lực và trường để liên hệ (012 FR-077), để Bác sĩ, Điều dưỡng chọn người thân khi ghi
-- phiếu nguyện vọng (BR-M01-16). Đối chiếu bằng PermissionMatrixMatchesSourceIT. Cú pháp dùng được ở MySQL 8.0 và 8.4.

INSERT INTO permission_matrix (role, function_code, operation, scope, footnote_codes, source)
VALUES ('AC-01', 'FAMILY_CONTACT_LIST', 'VIEW', 'FACILITY', '[]', 'APPENDIX_27'),
       ('AC-02', 'FAMILY_CONTACT_LIST', 'VIEW', 'FACILITY', '[]', 'APPENDIX_27'),
       ('AC-03', 'FAMILY_CONTACT_LIST', 'VIEW', 'FACILITY', '[]', 'APPENDIX_27'),
       ('AC-04', 'FAMILY_CONTACT_LIST', 'VIEW', 'FACILITY', '[]', 'APPENDIX_27'),
       ('AC-09', 'FAMILY_CONTACT_LIST', 'VIEW', 'FACILITY', '[]', 'APPENDIX_27');
