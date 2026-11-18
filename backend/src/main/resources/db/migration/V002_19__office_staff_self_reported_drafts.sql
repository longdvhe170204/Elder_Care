-- 001 T110 (Q-412, BR-M01-18, Phụ lục 27 dòng "Đánh giá, mức chăm sóc" và "Nguyện vọng cuối đời" cột Hành chính = T⁵²):
-- Hành chính lập bản Nháp tự khai khi người cao tuổi còn Đang tiếp nhận. Chú thích ⁵² do module resident kiểm
-- (SelfReportDraftFootnoteCondition); ma trận chỉ là quyền tối đa, phần hẹp hơn (chỉ Nháp mình lập, không xác nhận, không
-- kiểm chứng, không hủy) kiểm ở dịch vụ. Phiếu nguyện vọng khi điều trị theo dòng "Dị ứng, bệnh nền" đã có từ V002_14.
-- PermissionMatrixMatchesSourceIT đối chiếu từng ô với Phụ lục 27.
INSERT INTO permission_matrix (role, function_code, operation, scope, footnote_codes, source)
VALUES ('AC-09', 'ASSESSMENT_CARE_LEVEL', 'VIEW', 'FACILITY', '["52"]', 'APPENDIX_27'),
       ('AC-09', 'ASSESSMENT_CARE_LEVEL', 'CREATE', 'FACILITY', '["52"]', 'APPENDIX_27'),
       ('AC-09', 'ASSESSMENT_CARE_LEVEL', 'UPDATE', 'FACILITY', '["52"]', 'APPENDIX_27'),
       ('AC-09', 'END_OF_LIFE_WISHES', 'VIEW', 'FACILITY', '["52"]', 'APPENDIX_27'),
       ('AC-09', 'END_OF_LIFE_WISHES', 'CREATE', 'FACILITY', '["52"]', 'APPENDIX_27'),
       ('AC-09', 'END_OF_LIFE_WISHES', 'UPDATE', 'FACILITY', '["52"]', 'APPENDIX_27');
