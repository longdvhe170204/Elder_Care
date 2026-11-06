-- 001 T102 (Q-400, BR-M01-17, Phụ lục 27 dòng "Dị ứng, bệnh nền" cột Hành chính = T⁵⁰): Hành chính ghi tự khai của gia đình về
-- sức khỏe. Chú thích ⁵⁰ do module resident kiểm (Hành chính chỉ khi người cao tuổi còn Đang tiếp nhận); ma trận chỉ là quyền tối
-- đa, lệnh loại trừ mục, xác nhận hồ sơ vẫn chỉ Bác sĩ, Điều dưỡng (kiểm vai trò ở dịch vụ). Quy tắc xem trường của Hành chính
-- với nhóm sức khỏe giữ NONE (19.3). PermissionMatrixMatchesSourceIT đối chiếu từng ô với Phụ lục 27.
INSERT INTO permission_matrix (role, function_code, operation, scope, footnote_codes, source)
VALUES ('AC-09', 'ALLERGY_CONDITION', 'VIEW', 'FACILITY', '["50"]', 'APPENDIX_27'),
       ('AC-09', 'ALLERGY_CONDITION', 'CREATE', 'FACILITY', '["50"]', 'APPENDIX_27'),
       ('AC-09', 'ALLERGY_CONDITION', 'UPDATE', 'FACILITY', '["50"]', 'APPENDIX_27');
