-- BF-06: Nhân viên chăm sóc (AC-05) được tạo cảnh báo cho người cao tuổi trong phạm vi phụ trách (trước đây chỉ Trưởng tầng,
-- Bác sĩ, Điều dưỡng). Cảnh báo sức khỏe vẫn do Điều dưỡng trở lên tiếp nhận và đóng. Cần cập nhật Phụ lục 27 cho khớp.
INSERT INTO permission_matrix (role, function_code, operation, scope, footnote_codes, source)
VALUES ('AC-05', 'ALERT_HANDLING', 'CREATE', 'ASSIGNED', '[]', 'SECTION_19_3');
